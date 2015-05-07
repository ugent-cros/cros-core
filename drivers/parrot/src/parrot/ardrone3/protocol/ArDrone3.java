package parrot.ardrone3.protocol;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteIterator;
import akka.util.ByteString;
import parrot.ardrone3.models.*;
import parrot.ardrone3.handlers.ArDrone3TypeProcessor;
import parrot.ardrone3.handlers.CommonTypeProcessor;
import parrot.ardrone3.util.FrameHelper;
import parrot.ardrone3.util.PacketCreator;
import parrot.ardrone3.util.PacketHelper;
import parrot.shared.commands.*;
import parrot.shared.models.DroneConnectionDetails;
import droneapi.messages.ConnectionStatusChangedMessage;
import droneapi.messages.StopMessage;
import droneapi.model.properties.FlipType;
import parrot.shared.commands.MoveCommand;
import org.joda.time.DateTime;
import parrot.shared.util.H264Decoder;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
    private final static int PING_INTERVAL = 1000;

    private static final int MAX_FRAGMENT_SIZE = 1000; //max video fragment size, can be parsed from json
    private static final int MAX_FRAGMENT_NUM = 128;
    private static final int MAX_VIDEOBUFFER_SIZE = 4 * 1024 * 1024;

    // Receiving ID's
    private static final byte PING_CHANNEL = 0;
    private static final byte PONG_CHANNEL = 1;
    private static final byte NAVDATA_CHANNEL = 127;
    private static final byte EVENT_CHANNEL = 126;
    private static final byte VIDEO_DATA_CHANNEL = 125;

    private static final byte NONACK_CHANNEL = 10;
    private static final byte ACK_CHANNEL = 11;
    private static final byte EMERGENCY_CHANNEL = 12;
    private static final byte VIDEO_ACK = 13;

    private final EnumMap<FrameDirection, Map<Byte, DataChannel>> channels;
    private final List<DataChannel> ackChannels;
    private final Map<Byte, CommandTypeProcessor> processors;

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private InetSocketAddress senderAddress;
    private ActorRef senderRef;
    private int receivingPort;

    private final ActorRef listener; //to respond messages to

    private boolean isOffline = true;
    private long lastPong = 0;
    private long lastPing = 0;

    // Video processing
    private H264Decoder decoder;
    private byte[] fragmentBuffer;
    private int currentFrameSize;
    private int currentFrameNum;
    private static PipedInputStream pis;
    private static PipedOutputStream pos;
    private long lowPacketsAck;
    private long highPacketsAck;
    private boolean captureVideo;
    private long lastCmd = 0;

    public ArDrone3(int receivingPort, final ActorRef listener) {
        this.receivingPort = receivingPort;
        this.listener = listener;

        this.channels = new EnumMap<>(FrameDirection.class);
        this.ackChannels = new ArrayList<>();
        this.processors = new HashMap<>();

        initChannels(); // Initialize channels
        initHandlers(); //TODO: static lazy loading

        final ActorRef udpMgr = Udp.get(getContext().system()).getManager();
        udpMgr.tell(UdpMessage.bind(getSelf(), new InetSocketAddress(receivingPort)), getSelf());
        log.debug("Listening on [{}]", receivingPort);

        // Request a sender socket
        udpMgr.tell(UdpMessage.simpleSender(), getSelf());
    }


    @Override
    public void aroundPostStop() {
        super.aroundPostStop();
        if (senderRef != null) {
            senderRef.tell(new PoisonPill() {
            }, self()); // stop the sender
        }

        if (decoder != null) {
            decoder.setStop();
            decoder = null;
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.create("1 minute"),
                t -> {
                    log.error(t, "Bebop actor failure caught by supervisor.");
                    System.err.println(t.getMessage());
                    return SupervisorStrategy.resume(); // Continue on all exceptions!
                });
    }


    public boolean sendData(ByteString data) {
        if (senderAddress != null && senderRef != null) {
            if (data != null && data.length() != 0) {
                log.debug("Sending RAW data.");
                senderRef.tell(UdpMessage.send(data, senderAddress), getSelf());
                return true;
            } else {
                log.warning("Sending empty message.");
                return false;
            }
        } else {
            log.debug("Sending data without discovery data available.");
            return false;
        }
    }

    private void stop() {
        log.debug("Unbinding ARDrone 3 UDP listener.");
        if (senderRef != null) {
            senderRef.tell(UdpMessage.unbind(), self());
            senderRef = null;
        }
        getContext().stop(self());
    }

    private Packet extractPacket(Frame frame) {
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
                payload = frame.getData().slice(4, 4 + payloadLen);
            }
            return new Packet(type, cmdClass, cmd, payload);
        }
        return null;
    }

    private void processPacket(Packet packet) {
        if (packet == null)
            return;

        CommandTypeProcessor p = processors.get(packet.getType());
        if (p == null) {
            log.debug("No CommandTypeProcessor for [{}]", packet.getType());
        } else {
            try {
                Object msg = p.handle(packet);

                if (msg != null) {
                    log.debug("Sending message to listener actor: [{}]", msg.getClass().getCanonicalName());
                    listener.tell(msg, getSelf()); //Dispatch message back to droneactor
                }
            } catch (RuntimeException ex) {
                log.error(ex, "Packet handler failed ([{}], [{}], [{}]", packet.getType(), packet.getCommandClass(), packet.getCommand());
            }
        }
    }

    private void processDataFrame(Frame frame) {
        Map<Byte, DataChannel> recvMap = channels.get(FrameDirection.TO_CONTROLLER);
        DataChannel ch = recvMap.get(frame.getId());
        if (ch != null) {
            if (ch.shouldAllowFrame(frame)) {
                Packet packet = extractPacket(frame);
                log.debug("Packet received, Proj=[{}], Class=[{}], Cmd=[{}]", packet.getType(), packet.getCommandClass(), packet.getCommand());
                processPacket(packet);
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
            handlePong(frame.getData());
        } else {
            switch (frame.getType()) {
                case ACK:
                    processAck(frame);
                    break;
                case DATA:
                    processDataFrame(frame);
                    break;
                case DATA_LOW_LATENCY:
                    if (captureVideo) {
                        handleVideoData(frame);
                    }
                    break;
                case DATA_WITH_ACK:
                    processDataFrame(frame);
                    sendAck(frame); //ALWAYS send ack, even when seq is ignored
                    break;
                default:
                    log.warning("Invalid frame type handler; [{}]", frame.getType());
                    break;
            }
        }
    }

    private void flushFrame() {
        if (currentFrameSize > 0) {
            if (pos == null) {
                log.warning("PipedOutputStream is null.");
            } else if (fragmentBuffer == null) {
                log.warning("Empty fragment buffer.");
            } else {
                try {
                    pos.write(fragmentBuffer, 0, currentFrameSize);
                    pos.flush();
                } catch (Exception ex) {
                    log.error(ex, "Failed flushing bebop video frame.");
                }
            }
            currentFrameSize = 0;
        }
    }

    private void resetVideoChecksum(int fragmentsPerFrame) {
        // This code could possibly never work in Java due to long-long (128 bit) dependency in official sdk
        if (0 <= fragmentsPerFrame && fragmentsPerFrame < 64) {
            highPacketsAck = Long.MAX_VALUE;
            lowPacketsAck = Long.MAX_VALUE << fragmentsPerFrame;
        } else if (64 <= fragmentsPerFrame && fragmentsPerFrame < 128) {
            highPacketsAck = Long.MAX_VALUE << (fragmentsPerFrame - 64);
            lowPacketsAck = 0;
        } else {
            highPacketsAck = 0;
            lowPacketsAck = 0;
        }
    }

    private void handleVideoData(Frame dataFrame) {
        ByteString data = dataFrame.getData();
        ByteIterator it = data.iterator();
        int frameNum = it.getShort(FrameHelper.BYTE_ORDER);
        byte flags = it.getByte();
        byte fragNumSigned = it.getByte();
        int fragNum = fragNumSigned & 0xff; //make byte unsigned
        byte fragPerFrameSigned = it.getByte();
        int fragPerFrame = fragPerFrameSigned & 0xff; //make unsigned
        boolean flushFrame = (flags & 1) == 1; //check 1st bit, ignore for now?

        if (frameNum != currentFrameNum) {
            log.debug("Flush frame {}, size {}", currentFrameNum, currentFrameSize);
            flushFrame();

            resetVideoChecksum(fragPerFrame);
            currentFrameNum = frameNum;

            lowPacketsAck = 0;
            highPacketsAck = 0;
        }

        // Reassemble fragments to a frame buffer
        int offset = fragNum * MAX_FRAGMENT_SIZE;
        int dataLen = data.size() - 5; //minus length header
        if (fragNum == fragPerFrame - 1) { // final frame, perhaps check for flush, could be smaller frame than max size
            currentFrameSize = ((fragPerFrame - 1) * MAX_FRAGMENT_SIZE) + dataLen;
        } else if (dataLen != MAX_FRAGMENT_SIZE) {
            log.warning("Received incomplete video frame. len={}, maxlen={}", dataLen, MAX_FRAGMENT_SIZE);
        }
        it.getBytes(fragmentBuffer, offset, dataLen);
        log.debug("FrameNum={}, fragNum={}, numOfFrag={}, flush={}", frameNum, fragNum, fragPerFrame, flushFrame);

        // Set ack flags:
        if (0 <= fragNum && fragNum < 64) {
            lowPacketsAck |= (1 << fragNum);
        } else if (64 <= fragNum && fragNum < 128) {
            highPacketsAck |= (1 << (fragNum - 64));
        }

        // Now ack this video data
        DataChannel ch = channels.get(FrameDirection.TO_DRONE).get(VIDEO_ACK);
        Frame f = ch.createFrame(FrameHelper.getVideoAck(frameNum, lowPacketsAck, highPacketsAck));
        sendData(FrameHelper.getFrameData(f));
    }

    private void handlePong(ByteString data) {
        long now = System.currentTimeMillis();
        lastPong = now;

        long timeStamp = data.iterator().getLong(FrameHelper.BYTE_ORDER);
        long diff = now - timeStamp;
        log.debug("Pong received, RTT=[{}]ms.", diff);
        if (isOffline) {
            isOffline = false;
            listener.tell(new ConnectionStatusChangedMessage(true), getSelf());
        }
    }

    private void processAck(Frame frame) {
        byte realId = FrameHelper.getAckToServer(frame.getId());
        log.debug("Ack received for ID [{}]", realId);
        Map<Byte, DataChannel> recvMap = channels.get(FrameDirection.TO_DRONE);
        DataChannel ch = recvMap.get(realId);
        if (ch != null) {
            byte seq = frame.getData().iterator().getByte();
            long time = System.currentTimeMillis();
            Frame nextFrame = ch.receivedAck(seq, time);
            if (nextFrame != null) {
                log.debug("Advancing in ACK queue (recv = [{}]), sending seq=[{}]", seq, nextFrame.getSeq());
                sendData(FrameHelper.getFrameData(nextFrame));
            } else {
                log.debug("Advancing ACK, queue empty.");
            }
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

        Map<Byte, DataChannel> sendChannels = channels.get(FrameDirection.TO_DRONE);
        DataChannel ch = sendChannels.get(id);
        if (ch != null) {
            log.debug("Sending ACK for id = [{}]", frame.getId());
            ByteString payload = FrameHelper.getAck(frame);
            sendData(FrameHelper.getFrameData(ch.createFrame(payload))); // Send pong
        } else {
            log.warning("Could not find ACK channel for id = [{}]", frame.getId());
        }
    }

    private void processRawData(ByteString data) {
        if (data == null || data.length() == 0) {
            log.warning("Empty message received");
            return;
        }

        int numMsg = 0;
        while (true) {
            int len = data.length();
            if (len < 7) { // no header available
                break;
            } else {
                final int length = data.iterator().drop(3).getInt(FrameHelper.BYTE_ORDER); //skip first 3 bytes (type, id, seq)
                if (length > MAX_FRAME_SIZE) {
                    log.error("Received too large frame: [{}]", length);
                    throw new IllegalArgumentException(
                            "received too large frame of size " + length + " (max = "
                                    + MAX_FRAME_SIZE + ")");
                } else if (data.length() < length) {
                    log.warning("Received half a packet.");
                    break;
                } else {
                    ByteIterator it = data.iterator();
                    final byte type = it.getByte();
                    final byte id = it.getByte();
                    final byte seq = it.getByte();

                    ByteString payload = data.slice(7, length);
                    processFrame(new Frame(FrameHelper.parseFrameType(type), id, seq, payload));

                    numMsg++;
                    data = data.drop(length);
                }
            }
        }
        if (numMsg == 0)
            log.warning("Failed to extract any frame from packet.");
    }

    private void addSendChannel(FrameType type, byte id) {
        Map<Byte, DataChannel> sendChannels = channels.get(FrameDirection.TO_DRONE);
        DataChannel ch = new DataChannel(id, type, 0, 500, 3);
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
        recvChannels.put(id, new DataChannel(id, type, 0, 0, 3)); //TODO: specify send/recv so queue isn't needed
    }

    private void initChannels() {
        channels.put(FrameDirection.TO_CONTROLLER, new HashMap<>());
        channels.put(FrameDirection.TO_DRONE, new HashMap<>());

        // Init default recv channels
        addRecvChannel(FrameType.DATA, PING_CHANNEL);
        addRecvChannel(FrameType.DATA, PONG_CHANNEL);
        addRecvChannel(FrameType.DATA_WITH_ACK, EVENT_CHANNEL);
        addRecvChannel(FrameType.DATA, NAVDATA_CHANNEL);
        addRecvChannel(FrameType.DATA_LOW_LATENCY, VIDEO_DATA_CHANNEL);

        // Init default send channels
        addSendChannel(FrameType.DATA, PING_CHANNEL);
        addSendChannel(FrameType.DATA, PONG_CHANNEL);
        addSendChannel(FrameType.DATA, NONACK_CHANNEL);
        addSendChannel(FrameType.DATA_LOW_LATENCY, VIDEO_ACK);
        addSendChannel(FrameType.DATA_WITH_ACK, ACK_CHANNEL);
        addSendChannel(FrameType.DATA_WITH_ACK, EMERGENCY_CHANNEL);
    }

    private void initHandlers() {
        processors.put(PacketType.ARDRONE3.getVal(), new ArDrone3TypeProcessor());
        processors.put(PacketType.COMMON.getVal(), new CommonTypeProcessor());
    }

    @Override
    public void preStart() {
        log.info("Starting ARDrone 3.0 communication protocol. d2c={}", receivingPort);
        getContext().system().scheduler().scheduleOnce(
                Duration.create(TICK_DURATION, TimeUnit.MILLISECONDS),
                getSelf(), "tick", getContext().dispatcher(), null);
    }

    private void droneDiscovered(DroneConnectionDetails details) {
        if (this.senderAddress != null) {
            log.info("ArDrone3 protocol drone IP information updated: {}", details);
        }
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());
        log.debug("Enabled SEND at protocol level. Sending port=[{}]", details.getSendingPort());
        isOffline = false;

        lastPong = System.currentTimeMillis(); // reset ping timers
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            log.debug("Socket ARDRone 3.0 bound.");
            //senderRef = getSender();

            // Setup handlers
            getContext().become(ReceiveBuilder
                    .match(StopMessage.class, s -> stop())
                    .match(String.class, "tick"::equals, s -> tick())
                    .match(Udp.Received.class, s -> {
                        try {
                            processRawData(s.data());
                        } catch (Exception ex) {
                            log.error(ex, "Failed processing UDP frame.");
                        }
                    })
                    .match(Udp.Unbound.class, s -> {
                        log.info("UDP unbound received.");
                        getContext().stop(getSelf());
                    })
                    .match(Udp.SimpleSenderReady.class, s -> senderRef = sender())
                    .match(DroneConnectionDetails.class, s -> droneDiscovered(s))
                    .match(StopMessage.class, s -> {
                        log.info("ArDrone3 protocol stop received.");
                        stop();
                    })

                            // Drone commands
                    .match(FlatTrimCommand.class, s -> flatTrim())
                    .match(TakeOffCommand.class, s -> takeOff())
                    .match(LandCommand.class, s -> land())
                    .match(RequestStatusCommand.class, s -> requestStatus())
                    .match(SetOutdoorCommand.class, s -> setOutdoor(s.isOutdoor()))
                    .match(RequestSettingsCommand.class, s -> requestSettings())
                    .match(InitVideoCommand.class, s -> handleSetVideo(true))
                    .match(StopVideoCommand.class, s -> handleSetVideo(false))
                    .match(MoveCommand.class, s -> handleMove(s.getVx(), s.getVy(), s.getVz(), s.getVr()))
                    .match(FlipCommand.class, s -> handleFlip(s.getFlip()))
                    .match(SetDateCommand.class, s -> setDate(s.getDate()))
                    .match(SetTimeCommand.class, s -> setTime(s.getTime()))
                    .match(SetVideoStreamingStateCommand.class, s -> setVideoStreaming(s.isEnabled()))
                    .match(SetMaxHeightCommand.class, s -> setMaxHeight(s.getMeters()))
                    .match(SetMaxTiltCommand.class, s -> setMaxTilt(s.getDegrees()))
                    .match(SetHullCommand.class, s -> setHull(s.hasHull()))
                    .match(SetCountryCommand.class, s -> setCountry(s.getCountry()))
                    .match(SetHomeCommand.class, s -> setHome(s.getLatitude(), s.getLongitude(), s.getAltitude()))
                    .match(SetControllerStateCommand.class, s -> handleControllerState(s.isEnabled()))
                    .match(NavigateHomeCommand.class, s -> navigateHome(s.isStart()))
                    .matchAny(s -> {
                        log.warning("No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());
        } else if (msg instanceof DroneConnectionDetails) {
            droneDiscovered((DroneConnectionDetails) msg);
        } else if (msg instanceof StopMessage) {
            stop();
        } else if (msg instanceof Udp.SimpleSenderReady) {
            senderRef = sender();
        } else {
            unhandled(msg);
        }
    }

    private void startVideo() {
        if (decoder == null) {
            log.info("Starting video decoder for Bebop");
            try {
                pos = new PipedOutputStream();
                pis = new PipedInputStream(pos, MAX_VIDEOBUFFER_SIZE);
                fragmentBuffer = new byte[MAX_FRAGMENT_NUM * MAX_FRAGMENT_SIZE];

                H264Decoder decoder = new H264Decoder(pis, listener);
                decoder.start();
            } catch (Exception ex) {
                log.error(ex, "Failed to start video decoder.");
            }

            // TODO: these might be necessary right at the beginning as well to prevent video from crashing
            startVideoCommands();

            captureVideo = true;
        }
    }

    private void startVideoCommands() {
        setVideoStreaming(true);
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
        handleControllerState(true);
    }

    private void handleControllerState(boolean enabled) {
        sendDataAck(PacketCreator.createControllerStatePacket(enabled));
    }

    private void stopVideo() {
        if (decoder != null) {
            //setVideoStreaming(false);
            captureVideo = false;
            decoder.setStop(); //request stop
            decoder = null;
            fragmentBuffer = null; //release handle so GC can cleanup

            try {
                pos.close();
                pis.close();
            } catch (IOException ex) {
                log.error(ex, "Failed to close bebop video output streams.");
            }
        }
    }

    private void handleSetVideo(boolean enable) {
        if (enable && !captureVideo) {
            startVideo();
        } else if (!enable && captureVideo) {
            stopVideo();
        }
    }

    private void handleFlip(FlipType flip) {
        sendDataAck(PacketCreator.createFlipPacket(flip));
    }

    private void moveDrone(double vx, double vy, double vz, double vr) {
        boolean useRoll = (Math.abs(vx) > 0.0 || Math.abs(vy) > 0.0); // flag 1 if not hovering

        double[] vars = new double[]{vy, vx, vr, vz};
        for (int i = 0; i < 4; i++) {
            vars[i] *= 100; // multiplicator [-1;1] => [-100;100]

            if (Math.abs(vars[i]) > 100) {
                vars[i] = 100d * Math.signum(vars[i]); // never full
            }
        }

        /*
        Roll = vy = left-right, pitch = vx = front-back, vz = yaw = up-down, vr = rotation
        Quad reference: https://developer.valvesoftware.com/w/images/7/7e/Roll_pitch_yaw.gif

        The left-right tilt (aka. "drone roll" or phi angle) argument is a percentage of the maximum
        inclination as configured here. A negative value makes the drone tilt to its left, thus flying
        leftward. A positive value makes the drone tilt to its right, thus flying rightward.

        The front-back tilt (aka. "drone pitch" or theta angle) argument is a percentage of the maximum
        inclination as configured here. A negative value makes the drone lower its nose, thus flying
        frontward. A positive value makes the drone raise its nose, thus flying backward.

        The drone translation speed in the horizontal plane depends on the environment and cannot
        be determined. With roll or pitch values set to 0, the drone will stay horizontal but continue
        sliding in the air because of its inertia. Only the air resistance will then make it stop.

        The vertical speed (aka. "gaz") argument is a percentage of the maximum vertical speed as
        defined here. A positive value makes the drone rise in the air. A negative value makes it go
        down.

        The angular speed argument is a percentage of the maximum angular speed as defined here.
        A positive value makes the drone spin right; a negative value makes it spin left.
         */

        sendDataNoAck(PacketCreator.createMove3dPacket(useRoll, (byte) vars[0], (byte) vars[1], (byte) vars[2], (byte) vars[3]));
    }

    private void handleMove(double vx, double vy, double vz, double vr) {
        log.debug("ArDrone3 MOVE command [vx=[{}], vy=[{}], vz=[{}], vr=[{}]", vx, vy, vz, vr);
        moveDrone(vx, vy, vz, vr);

        lastCmd = System.currentTimeMillis();
    }

    private void handleSetOrientation(byte tilt, byte pan) {
        sendDataNoAck(PacketCreator.createOrientationPacket(tilt, pan));
    }

    private void checkPing(long time) {
        // When not discovered yet
        if (senderAddress == null || senderRef == null)
            return;

        if (lastPing > 0 && time - lastPong > 3 * PING_INTERVAL) {
            if (!isOffline) {
                isOffline = true;
                listener.tell(new ConnectionStatusChangedMessage(false), getSelf());
            }
        }

        if (time - lastPing > PING_INTERVAL) {
            DataChannel pingChannel = channels.get(FrameDirection.TO_DRONE).get(PING_CHANNEL);
            if (pingChannel != null) {
                Frame f = pingChannel.createFrame(PacketHelper.getPingPacket(time));
                if (sendData(FrameHelper.getFrameData(f))) {
                    lastPing = time;
                    log.debug("Sent ping at [{}]", time);
                } else log.warning("Failed to sent ping.");
            } else {
                log.error("No PING channel defined.");
            }
        }
    }


    private void tick() {
        long time = System.currentTimeMillis();
        try {
            checkPing(time);
            for (DataChannel ch : ackChannels) {
                Frame f = ch.tick(time);
                if (f != null) {
                    sendData(FrameHelper.getFrameData(f)); //TODO: only compute once
                }
            }
        } catch (Exception ex) {
            log.warning("Failed to process ArDrone3 timer tick.");
        }

        if(senderRef != null && time - lastCmd > 800) {
            sendDataNoAck(PacketCreator.createMove3dPacket(false, (byte)0, (byte)0, (byte)0, (byte)0)); // movement keep-alive?
        }

        // Reschedule
        getContext().system().scheduler().scheduleOnce(
                Duration.create(TICK_DURATION, TimeUnit.MILLISECONDS),
                getSelf(), "tick", getContext().dispatcher(), null);
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
    private void flatTrim() {
        sendDataAck(PacketCreator.createFlatTrimPacket());
    }

    private void takeOff() {
        lastCmd = System.currentTimeMillis() + 1000; //wait for 1s + extra to send move commands
        sendDataAck(PacketCreator.createTakeOffPacket());
    }

    private void land() {
        sendDataAck(PacketCreator.createLandingPacket());
    }

    private void requestStatus() {
        sendDataAck(PacketCreator.createRequestStatusPacket());
    }

    private void setVideoStreaming(boolean enabled) {
        sendDataAck(PacketCreator.createSetVideoStreamingStatePacket(enabled));
    }

    private void requestSettings() {
        sendDataAck(PacketCreator.createRequestAllSettingsCommand());
    }

    private void setOutdoor(boolean outdoor) {
        sendDataAck(PacketCreator.createOutdoorStatusPacket(outdoor));
    }

    private void setMaxHeight(float meters) {
        sendDataAck(PacketCreator.createSetMaxAltitudePacket(meters));
    }

    private void setMaxTilt(float degrees) {
        sendDataAck(PacketCreator.createSetMaxTiltPacket(degrees));
    }

    private void setHull(boolean hull) {
        sendDataAck(PacketCreator.createSetHullPacket(hull));
    }

    private void setCountry(String ctry) {
        sendDataAck(PacketCreator.createSetCountryPacket(ctry));
    }

    private void setHome(double latitude, double longitude, double altitude) {
        sendDataAck(PacketCreator.createSetHomePacket(latitude, longitude, altitude));
    }

    private void setDate(DateTime time) {
        sendDataAck(PacketCreator.createCurrentDatePacket(time));
    }

    private void setTime(DateTime time) {
        sendDataAck(PacketCreator.createCurrentTimePacket(time));
    }

    private void navigateHome(boolean start) {
        sendDataAck(PacketCreator.createNavigateHomePacket(start));
    }

}
