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
import drones.models.*;
import drones.util.FrameHelper;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Cedric on 3/6/2015.
 */
public class ArDrone3 extends UntypedActor {

    private static final int MAX_FRAME_SIZE = 1500; //TODO check

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

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;
    private ActorRef connectionMgrRef;

    //http://doc.akka.io/docs/akka/2.2.0/java/io-codec.html
    private ByteString recvBuffer;

    public ArDrone3(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());

        this.channels = new EnumMap<>(FrameDirection.class);

        initChannels(); // Initialize channels

        final ActorRef mgr = Udp.get(getContext().system()).getManager();
        mgr.tell(
                UdpMessage.bind(getSelf(), new InetSocketAddress("localhost", details.getReceivingPort())),
                getSelf());
    }

    @Override
    public void preStart() {
        log.info("Starting ARDrone 3.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            this.connectionMgrRef = getSender();
            getContext().become(ready(connectionMgrRef));
            log.debug("Socket ARDRone 3.0 bound.");
        } else unhandled(msg);
    }

    public void sendData(ByteString data) {
        connectionMgrRef.tell(UdpMessage.send(data, senderAddress), getSelf());
    }

    private void processDataFrame(Frame frame){
        Map<Byte, DataChannel> recvMap = channels.get(FrameDirection.TO_CONTROLLER);
        DataChannel ch = recvMap.get(frame.getId());
        if (ch != null) {
            if(ch.shouldAllowFrame(frame)){

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
        if(ch != null){
            sendData(FrameHelper.getFrameData(ch.createFrame(payload))); // Send pong
        } else{
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
        sendChannels.put(id, new DataChannel(id, type));
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

    private Procedure<Object> ready(final ActorRef socket) {
        return msg -> {
            if (msg instanceof Udp.Received) {
                final Udp.Received r = (Udp.Received) msg;
                //socket.tell(UdpMessage.send(r.data(), r.sender()), getSelf());
                processRawData(r.data());
            } else if (msg.equals(UdpMessage.unbind())) {
                socket.tell(msg, getSelf());

            } else if (msg instanceof Udp.Unbound) {
                getContext().stop(getSelf());

            } else unhandled(msg);
        };
    }
}
