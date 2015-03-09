package drones.protocols;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.Procedure;
import akka.util.ByteIterator;
import akka.util.ByteString;
import drones.commands.*;
import drones.handlers.ardrone3.ArDrone3TypeProcessor;
import drones.handlers.ardrone3.CommonTypeProcessor;
import drones.models.*;
import drones.models.ardrone3.*;
import drones.util.ardrone3.FrameHelper;
import drones.util.ardrone3.PacketCreator;
import drones.util.ardrone3.PacketHelper;
import scala.concurrent.duration.Duration;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Cedric on 3/6/2015.
 */
public class ArDrone3 extends UntypedActor {

    private static final int MAX_FRAME_SIZE = 1500; //TODO check
    private final static int TICK_DURATION = 50; //ms

    // Receiving ID's
    private static final byte PING_CHANNEL = 0;
    private static final byte PONG_CHANNEL = 1;
    private static final byte NAVDATA_CHANNEL = 127;
    private static final byte EVENT_CHANNEL = 126;
    private static final byte VIDEO_DATA_CHANNEL = 125;

    private static final byte NONACK_CHANNEL = 10;
    private static final byte ACK_CHANNEL = 11;
    private static final byte EMERGENCY_CHANNEL = 12;

    private final EnumMap<FrameDirection, Map<Byte, DataChannel>> channels;
    private final List<DataChannel> ackChannels;
    private final Map<Byte, CommandTypeProcessor> processors;

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;
    private ActorRef connectionMgrRef;

    private ByteString recvBuffer;

    private final ActorRef listener; //to respond messages to

    public ArDrone3(DroneConnectionDetails details, final ActorRef listener) {
        this.details = details;
        this.listener = listener;

        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());

        this.channels = new EnumMap<>(FrameDirection.class);
        this.ackChannels = new ArrayList<>();
        this.processors = new HashMap<>();

        initChannels(); // Initialize channels
        initHandlers(); //TODO: static lazy loading

        final ActorRef mgr = Udp.get(getContext().system()).getManager();
        mgr.tell(
                UdpMessage.bind(getSelf(), new InetSocketAddress("localhost", details.getReceivingPort())),
                getSelf());
    }

    public void sendData(ByteString data) {
        connectionMgrRef.tell(UdpMessage.send(data, senderAddress), getSelf());
    }

    private void extractPacket(Frame frame) {
        ByteIterator it = frame.getData().iterator();
        byte type = it.getByte();
        byte cmdClass = it.getByte();
        short cmd = it.getShort(FrameHelper.BYTE_ORDER);
        if (cmd < 0) {
            log.warning("Command sign bit overflow.");
        } else {
            int payloadLen = frame.getData().length() - 4;
            ByteString payload = null;
            if (payloadLen > 0) {
                payload = frame.getData().slice(4, payloadLen);
            }
            Packet packet = new Packet(type, cmdClass, cmd, payload);
            processPacket(packet);
        }
    }

    private void processPacket(Packet packet) {
        CommandTypeProcessor p = processors.get(packet.getType());
        if (p == null) {
            log.debug("No CommandTypeProcessor for [{}]", p.getType());
        }
        Object msg = p.handle(packet);

        if (msg != null) {
            listener.tell(msg, getSelf()); //Dispatch message
        }
    }

    private void processDataFrame(Frame frame) {
        Map<Byte, DataChannel> recvMap = channels.get(FrameDirection.TO_CONTROLLER);
        DataChannel ch = recvMap.get(frame.getId());
        if (ch != null) {
            if (ch.shouldAllowFrame(frame)) {
                extractPacket(frame);
            } else {
                log.warning("Packet timed out in seq.");
            }
        } else {
            log.warning("Received packet on unknown channel: [{}], type=[{}]", frame.getId(), frame.getType());
        }
    }

    private void processFrame(Frame frame) {
        log.debug("Processing frame: type = [{}], id = [{}], seq = [{}]", frame.getType(), frame.getId(), frame.getSeq());

        if (frame.getId() == PING_CHANNEL) {
            sendPong(frame);
        } else if (frame.getId() == PONG_CHANNEL) {
            log.debug("Pong received.");
            //TODO
        } else {
            switch (frame.getType()) {
                case ACK:
                    processAck(frame);
                    break;
                case DATA:
                case DATA_LOW_LATENCY:
                    processDataFrame(frame);
                    break;
                case DATA_WITH_ACK:
                    processDataFrame(frame);
                    sendAck(frame); //ALWAYS send ack, even when seq is ignored
                    break;
            }
        }
    }

    private void processAck(Frame frame) {
        byte realId = FrameHelper.getAckToServer(frame.getId());
        Map<Byte, DataChannel> recvMap = channels.get(FrameDirection.TO_CONTROLLER);
        DataChannel ch = recvMap.get(realId);
        if (ch != null) {
            byte seq = frame.getData().iterator().getByte();
            ch.receivedAck(seq);
        } else {
            log.warning("Received ack for unknown channel id: [{}]", realId);
        }
    }

    private void sendPong(Frame pingPacket) {
        //Note: there is a bug in the drone PING packet only containing the seconds
        DataChannel ch = channels.get(FrameDirection.TO_DRONE).get(PONG_CHANNEL);
        ByteIterator it = pingPacket.getData().iterator();
        long ping = it.getLong(ByteOrder.LITTLE_ENDIAN);
        log.debug("Ping: [{}]", ping);

        ByteString pongPacket = FrameHelper.getPong(ping);

        sendData(FrameHelper.getFrameData(ch.createFrame(pongPacket))); // Send pong
    }

    private void sendAck(Frame frame) {
        byte id = FrameHelper.getAckToDrone(frame.getId());
        ByteString payload = FrameHelper.getAck(frame);
        Map<Byte, DataChannel> sendChannels = channels.get(FrameDirection.TO_DRONE);
        DataChannel ch = sendChannels.get(id);
        if (ch != null) {
            sendData(FrameHelper.getFrameData(ch.createFrame(payload))); // Send pong
        } else {
            log.warning("Could not find ACK channel for id = [{}]", frame.getId());
        }
    }

    private void processRawData(ByteString data) {
        ByteString current = recvBuffer == null ? data : recvBuffer.concat(data); //immutable rolling buffer

        while (true) {
            int len = current.length();
            if (len == 0) {
                recvBuffer = null;
                break;
            } else if (len < 7) {
                recvBuffer = current;
                break;
            } else {
                final int length = current.iterator().drop(3).getInt(FrameHelper.BYTE_ORDER); //skip first 3 bytes (type, id, seq)
                if (length > MAX_FRAME_SIZE) {
                    log.error("Received too large frame: [{}]", length);
                    throw new IllegalArgumentException(
                            "received too large frame of size " + length + " (max = "
                                    + MAX_FRAME_SIZE + ")");
                } else if (current.length() < length) {
                    recvBuffer = current;
                    break;
                } else {
                    ByteIterator it = current.iterator();
                    final byte type = it.getByte();
                    final byte id = it.getByte();
                    final byte seq = it.getByte();

                    ByteString payload = current.slice(7, length);
                    processFrame(new Frame(FrameHelper.parseFrameType(type), id, seq, payload));

                    current = current.drop(length);
                }
            }
        }

    }

    private void addSendChannel(FrameType type, byte id) {
        Map<Byte, DataChannel> sendChannels = channels.get(FrameDirection.TO_DRONE);
        DataChannel ch = new DataChannel(id, type);
        if (type == FrameType.DATA_WITH_ACK) {
            ackChannels.add(ch);
        }
        sendChannels.put(id, ch);
    }

    private void addRecvChannel(FrameType type, byte id) {
        Map<Byte, DataChannel> recvChannels = channels.get(FrameDirection.TO_CONTROLLER);
        if (type == FrameType.DATA_WITH_ACK) { //create a send ack channel
            byte ackChannelId = FrameHelper.getAckToDrone(id);
            Map<Byte, DataChannel> sendChannels = channels.get(FrameDirection.TO_DRONE);
            sendChannels.put(ackChannelId, new DataChannel(ackChannelId, FrameType.ACK));
        }
        recvChannels.put(id, new DataChannel(id, type));
    }

    private void initChannels() {
        channels.put(FrameDirection.TO_CONTROLLER, new HashMap<>());
        channels.put(FrameDirection.TO_DRONE, new HashMap<>());

        // Init default recv channels
        addRecvChannel(FrameType.DATA, PING_CHANNEL);
        addRecvChannel(FrameType.DATA, PONG_CHANNEL);
        addRecvChannel(FrameType.DATA, EVENT_CHANNEL);
        addRecvChannel(FrameType.DATA, NAVDATA_CHANNEL);
        addRecvChannel(FrameType.DATA_LOW_LATENCY, VIDEO_DATA_CHANNEL);

        // Init default send channels
        addSendChannel(FrameType.DATA, PING_CHANNEL);
        addSendChannel(FrameType.DATA, PONG_CHANNEL);
        addSendChannel(FrameType.DATA, NONACK_CHANNEL);
        addSendChannel(FrameType.DATA_WITH_ACK, ACK_CHANNEL);
        addSendChannel(FrameType.DATA_WITH_ACK, EMERGENCY_CHANNEL);
    }

    private void initHandlers() {
        processors.put(PacketType.ARDRONE3.getVal(), new ArDrone3TypeProcessor());
        processors.put(PacketType.COMMON.getVal(), new CommonTypeProcessor());
    }

    @Override
    public void preStart() {
        log.info("Starting ARDrone 3.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
        getContext().system().scheduler().scheduleOnce(
                Duration.create(TICK_DURATION, TimeUnit.MILLISECONDS),
                getSelf(), "tick", getContext().dispatcher(), null);
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            this.connectionMgrRef = getSender();
            getContext().become(ready(connectionMgrRef));
            log.debug("Socket ARDRone 3.0 bound.");
        } else unhandled(msg);
    }

    private void dispatchCommand(DroneCommandMessage msg) {
        try {
            // Little dirty trick with reflection to avoid instanceof, can be optimized using lazy caching
            Method handler = ArDrone3.class.getMethod("handle", msg.getMessage().getClass());
            handler.invoke(null, msg.getMessage());
        } catch (Exception e) {
            log.warning("No command dispatch for [{}] command.", msg.getMessage().getClass().getCanonicalName());
        }
    }

    private void tick() {
        long time = System.currentTimeMillis();
        for (DataChannel ch : ackChannels) {
            Frame f = ch.tick(time);
            if (f != null) {
                sendData(FrameHelper.getFrameData(f)); //TODO: only compute once
            }
        }

        // Reschedule
        getContext().system().scheduler().scheduleOnce(
                Duration.create(TICK_DURATION, TimeUnit.MILLISECONDS),
                getSelf(), "tick", getContext().dispatcher(), null);
    }

    private Procedure<Object> ready(final ActorRef socket) {
        return msg -> {
            if (msg instanceof Udp.Received) {
                final Udp.Received r = (Udp.Received) msg;
                processRawData(r.data());
            } else if (msg.equals("tick")) {

            } else if (msg.equals(UdpMessage.unbind())) {
                socket.tell(msg, getSelf());
            } else if (msg instanceof Udp.Unbound) {
                getContext().stop(getSelf());
            } else if (msg instanceof DroneCommandMessage) {
                dispatchCommand((DroneCommandMessage) msg);
            } else unhandled(msg);
        };
    }

    private void sendDataOnChannel(Packet packet, DataChannel channel) {
        ByteString data = PacketHelper.buildPacket(packet);
        Frame frame = channel.createFrame(data);
        if (channel.getType() == FrameType.DATA_WITH_ACK) {
            long time = System.currentTimeMillis();
            Frame f = channel.sendFrame(frame, time);
            if (f != null) {
                sendData(FrameHelper.getFrameData(f));//TODO: only compute once
            }
        } else if (channel.getType() == FrameType.DATA) {
            sendData(FrameHelper.getFrameData(frame));
            log.debug("Sent packet ([{}], [{}], [{}]) on channel [{}]",
                    packet.getType(), packet.getCommandClass(), packet.getCommand(), channel.getId());
        } else {
            log.warning("Sending data over invalid channel type. ID = [{}]", channel.getId());
        }
    }

    private void sendDataAck(Packet packet) {
        DataChannel channel = channels.get(FrameDirection.TO_DRONE).get(ACK_CHANNEL);
        sendDataOnChannel(packet, channel);
    }

    private void sendDataEmergency(Packet packet) {
        DataChannel channel = channels.get(FrameDirection.TO_DRONE).get(EMERGENCY_CHANNEL);
        sendDataOnChannel(packet, channel);
    }

    private void sendDataNoAck(Packet packet) {
        DataChannel channel = channels.get(FrameDirection.TO_DRONE).get(NONACK_CHANNEL);
        sendDataOnChannel(packet, channel);
    }

    // All command handlers
    //TODO: move these to seperate class statically
    private void handle(FlatTrimCommand cmd) {
        sendDataNoAck(PacketCreator.createFlatTrimPacket());
    } //TODO: ack channel

    private void handle(TakeOffCommand cmd) {
        sendDataNoAck(PacketCreator.createTakeOffPacket()); //TODO: ACK channel
    }

    private void handle(LandCommand cmd) {
        sendDataNoAck(PacketCreator.createLandingPacket()); //TODO: ack channel
    }

    private void handle(RequestStatusCommand cmd) {
        sendDataNoAck(PacketCreator.createRequestStatusPacket()); //TODO: ack?
    }

    private void handle(OutdoorCommand cmd) {
        sendDataNoAck(PacketCreator.createOutdoorStatusPacket(cmd.isOutdoor())); //TODO: ack channel
    }
}
