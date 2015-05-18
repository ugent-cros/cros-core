package parrot.ardrone2.protocol;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import droneapi.model.properties.FlyingState;
import droneapi.model.properties.NavigationState;
import droneapi.model.properties.NavigationStateReason;
import parrot.ardrone2.commands.*;
import parrot.ardrone2.util.ConfigKey;
import parrot.ardrone2.util.DefaultPorts;
import parrot.messages.InitCompletedMessage;
import parrot.messages.InitNavDataMessage;
import parrot.messages.RequestConfigMessage;
import parrot.messages.VideoFailedMessage;
import parrot.shared.commands.*;
import parrot.shared.models.DroneConnectionDetails;
import parrot.ardrone2.util.PacketCreator;
import droneapi.messages.*;
import droneapi.model.properties.FlipType;
import parrot.shared.commands.MoveCommand;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * Created by brecht on 3/7/15.
 */
public class ArDrone2Protocol extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;

    private final ActorRef udpManager;
    private final ActorRef listener;

    // UDP connection details
    private DroneConnectionDetails details;
    private InetSocketAddress senderAddressATC;

    // Sequence number of command
    private int seq = 0;

    private boolean videoInited = false;

    // Session IDs
    private static final String ARDRONE_SESSION_ID     = "d2e081a3";  // SessionID
    private static final String ARDRONE_PROFILE_ID     = "be27e2e4";  // Profile ID
    private static final String ARDRONE_APPLOCATION_ID = "d87f7e0c";  // Application ID

    private static final int ALTITUDE_MAX = 3000;
    private static final int VZ_MAX = 1000;
    private static final double YAW_MAX = 30;

    private static final int REF_BIT_FIELD = (1 << 18) | (1 << 20) | (1 << 22) | (1 << 24) | (1 << 28);

    // Watchdog reset actor
    private ActorRef ardrone2ResetWDG;
    private ActorRef ardrone2NavData;
    private ActorRef ardrone2Config;
    private ActorRef ardrone2Video;

    private Object lock = new Object();

    public ArDrone2Protocol(DroneConnectionDetails details, final ActorRef listener) {
        // Connection details
        this.details = details;
        // ArDrone 2 Model
        this.listener = listener;
        // UPD manager
        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress(0)), getSelf());

        // Request a sender socket
        udpManager.tell(UdpMessage.simpleSender(), getSelf());

        log.info("[ARDRONE2] Starting ARDrone 2.0 Protocol");
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            log.info("[ARDRONE2] Socket ARDRone 2.0 bound.");

            // Setup handlers
            getContext().become(ReceiveBuilder
                    //.match(Udp.Unbound.class, s -> getContext().stop(getSelf()))
                    .match(Udp.Unbound.class, s -> {
                        log.info("UDP unbound received.");
                        getContext().stop(getSelf());
                    })

                    .match(Udp.SimpleSenderReady.class, s -> senderRef = sender())
                    .match(DroneConnectionDetails.class, s -> droneDiscovered(s))
                    .match(StopMessage.class, s -> stop())
                    .match(InitNavDataMessage.class, s -> sendInitNavData())
                    .match(VideoFailedMessage.class, s -> handleStopVideo())
                            // Drone commands
                    .match(InitDroneCommand.class, s -> handleInit())
                    .match(CalibrateCommand.class, s -> handleCalibrate())
                    .match(ResetCommand.class, s -> handleEmergencyReset())
                    .match(FlatTrimCommand.class, s -> handleFlatTrim())
                    .match(TakeOffCommand.class, s -> handleTakeoff())
                    .match(LandCommand.class, s -> handleLand())
                    .match(SetOutdoorCommand.class, s -> handleOutdoor(s))
                    .match(SetHullCommand.class, s -> handleSetHull(s.hasHull()))
                    .match(MoveCommand.class, s -> handleMove(s))
                    .match(SetMaxHeightCommand.class, s -> handleSetMaxHeight(s.getMeters()))
                    .match(StopMoveMessage.class, s -> handleStopMove())
                    .match(RequestConfigMessage.class, s -> handleConfigRequest())
                    .match(SetMaxTiltCommand.class, s -> handleMaxTilt(s))
                    .match(FlipCommand.class, s -> handleFlip(s.getFlip()))
                    .match(InitVideoCommand.class, s -> handleInitVideo())
                    .match(StopVideoCommand.class, s -> handleStopVideo())
                    .match(EmergencyCommand.class, s -> handleEmergencyReset())

                    .matchAny(s -> {
                        log.warning("[ARDRONE2] No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());

            listener.tell(new InitCompletedMessage(), getSelf());

            Object batteryMessage = new BatteryPercentageChangedMessage((byte) 15);
            listener.tell(batteryMessage, getSelf());
        } else if (msg instanceof DroneConnectionDetails) {
            log.info("[ARDRONE2] DroneConnectionDetails received");
            droneDiscovered((DroneConnectionDetails) msg);
        } else if (msg instanceof StopMessage) {
            log.info("[ARDRONE2] Stop message received - ArDrone2 protocol");
            stop();
        } else if(msg instanceof Udp.SimpleSenderReady){
            senderRef = sender();
        } else {
            log.info("[ARDRONE2] Unhandled message received - ArDrone2 protocol");
            unhandled(msg);
        }
    }

    private void handleStopVideo() {
        if (ardrone2Video != null) {
            ardrone2Video.tell(new StopMessage(), self());
        }

        videoInited = false;
        ardrone2Video = null;
    }

    private void handleEmergencyReset() {
        Object stateMessage = new FlyingStateChangedMessage(FlyingState.EMERGENCY);
        listener.tell(stateMessage, getSelf());
        
        int bitFields = (1 << 8) | REF_BIT_FIELD;
        sendData(PacketCreator.createPacket(new ATCommandREF(seq++, bitFields)));
    }

    private void handleInitVideo() {
        // Video configs
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.VIDEO_BITRATE_CTRL_MODE,
                Integer.toString(0))));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.VIDEO_BITRATE,
                Integer.toString(1000))));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.VIDEO_BITRATE_MAX,
                Integer.toString(4000))));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.VIDEO_CODEC,
                "0x81")));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.VIDEO_CHANNEL,
                "0")));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                ConfigKey.VIDEO_ON_USB, Boolean.toString(false).toUpperCase())));

        synchronized (lock) {
            if (!videoInited) {
                videoInited = true;
                // Create config data actor
                ardrone2Video = getContext().actorOf(Props.create(ArDrone2Video.class,
                        () -> new ArDrone2Video(details, listener, getSelf())));
            }
        }
    }

    @Override
    public void aroundPostStop() {
        super.aroundPostStop();
        if(senderRef != null){
            senderRef.tell(new PoisonPill(){}, self()); // stop the sender
        }
    }

    private void handleFlip(FlipType flip) {
        ATCommandANIM.AnimationID anim;
        switch(flip) {
            case BACK:
                anim = ATCommandANIM.AnimationID.FLIP_BEHIND;
                break;
            case FRONT:
                anim = ATCommandANIM.AnimationID.FLIP_AHEAD;
                break;
            case LEFT:
                anim = ATCommandANIM.AnimationID.FLIP_LEFT;
                break;
            default: // Flip right
                anim = ATCommandANIM.AnimationID.FLIP_RIGHT;
                break;
        }

        sendData(PacketCreator.createPacket(new ATCommandANIM(seq++, anim, 1)));
    }

    private void handleMaxTilt(SetMaxTiltCommand s) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.CONTROL_EULER_ANGLE_MAX,
                Float.toString(s.getDegrees()))));
    }

    private void handleStopMove() {
        sendData(PacketCreator.createPacket(new ATCommandPCMD(seq++, 0, -0f, -0f, 0f, -0f)));
    }

    private void handleCalibrate() {
        log.info("[ARDRONE2] Calibrate");
        sendData(PacketCreator.createPacket(new ATCommandCALIB(seq++)));
    }

    private void handleTakeoff() {
        log.info("[ARDRONE2] TakeOff");

        // 9th bit is takeoff bit
        int bitFields = (1 << 9) | REF_BIT_FIELD;
        sendData(PacketCreator.createPacket(new ATCommandREF(seq++, bitFields)));

        getContext().system().scheduler().scheduleOnce(Duration.create(250, TimeUnit.MILLISECONDS),
                getSelf(), new CalibrateCommand(), getContext().system().dispatcher(), null);
    }

    private void handleLand() {
        log.info("[ARDRONE2] Land");

        sendData(PacketCreator.createPacket(new ATCommandREF(seq++, REF_BIT_FIELD)));
    }

    private void handleConfigRequest() {
        log.info("[ARDRONE] Config file requested");

        sendData(PacketCreator.createPacket(new ATCommandCONTROL(seq++, 5, 0)));
        sendData(PacketCreator.createPacket(new ATCommandCONTROL(seq++, 4, 0)));
    }

    /**
     * For the init code see: https://github.com/puku0x/cvdrone/blob/master/src/ardrone/command.cpp
     */
    private void handleInit() {
        log.info("[ARDRONE2] Initializing ARDrone 2.0");

        sendData(PacketCreator.createPacket(new ATCommandPMODE(seq++, 2))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandMISC(seq++, 2, 20, 2000, 3000))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandFTRIM(seq++)));

        // Set the sessions
        // Set the configuration IDs
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.CUSTOM_SESSION_ID, ARDRONE_SESSION_ID)));
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.CUSTOM_PROFILE_ID, ARDRONE_PROFILE_ID)));
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.CUSTOM_APPLICATION_ID, ARDRONE_APPLOCATION_ID)));

        sendData(PacketCreator.createPacket(new ATCommandCOMWDG(seq++)));

        // 3m max height
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, ConfigKey.CONTROL_ALTITUDE_MAX,
                Integer.toString(ALTITUDE_MAX))));

        // max 30 deg yaw
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.CONTROL_CONTROL_YAW,
                Float.toString((float) Math.toRadians(YAW_MAX)))));

        // max 1m/s vertical speed
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, ConfigKey.CONTROL_CONTROL_VZ_MAX,
                Integer.toString(VZ_MAX))));

        // Create watchdog actor
        ardrone2ResetWDG = getContext().actorOf(Props.create(ArDrone2ResetWDG.class,
                () -> new ArDrone2ResetWDG(details)));

        // Create nav data actor
        ardrone2NavData = getContext().actorOf(Props.create(ArDrone2NavData.class,
                () -> new ArDrone2NavData(details, listener, getSelf())));


        // Create config data actor
        ardrone2Config = getContext().actorOf(Props.create(ArDrone2Config.class,
                () -> new ArDrone2Config(details, listener, getSelf())));
    }

    private void handleFlatTrim() {
        sendData(PacketCreator.createPacket(new ATCommandFTRIM(seq++)));
    }

    private void handleSetHull(boolean hull) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                ConfigKey.CONTROL_FLIGHT_WITHOUT_SHELL, Boolean.toString(!hull).toUpperCase())));
    }

    private void handleSetMaxHeight(float meters) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                ConfigKey.CONTROL_ALTITUDE_MAX, Integer.toString(Math.round(meters * 1000)))));
    }

    private void handleMove(MoveCommand s) {
        if(isMoveParamInRange((float) s.getVx()) && isMoveParamInRange((float) s.getVy())
                && isMoveParamInRange((float) s.getVz()) && isMoveParamInRange((float) s.getVr())) {

            float[] v = {0.2f * (float) s.getVy(), -0.2f * (float) s.getVx(),
                    1.0f * (float) s.getVz(), 1.0f * (float) s.getVr()};
            boolean mode = Math.abs(v[0]) > 0.0 || Math.abs(v[1]) > 0.0;

            // Normalization (-1.0 to +1.0)
            for (int i = 0; i < 4; i++) {
                if (Math.abs(v[i]) > 1.0) {
                    v[i] /= Math.abs(v[i]);
                }
            }

            sendData(PacketCreator.createPacket(new ATCommandPCMD(seq++, mode ? 1 : 0,
                    v[0], v[1], v[2], v[3])));
            getContext().system().scheduler().scheduleOnce(Duration.create(1000, TimeUnit.MILLISECONDS),
                    getSelf(), new StopMoveMessage(), getContext().system().dispatcher(), null);
        }
    }

    private void handleOutdoor(SetOutdoorCommand cmd) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                ConfigKey.CONTROL_OUTDOOR, Boolean.toString(cmd.isOutdoor()).toUpperCase())));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                ConfigKey.CONTROL_FLIGHT_WITHOUT_SHELL, Boolean.toString(cmd.isOutdoor()).toUpperCase())));
    }

    private ATCommandCONFIGIDS createConfigIDS(int seq) {
        return new ATCommandCONFIGIDS(seq, ARDRONE_SESSION_ID, ARDRONE_PROFILE_ID, ARDRONE_APPLOCATION_ID);
    }

    private void stop() {
        ardrone2ResetWDG.tell(new StopMessage(), self());
        ardrone2NavData.tell(new StopMessage(), self());
        ardrone2Config.tell(new StopMessage(), self());
        if(ardrone2Video != null) {
            ardrone2Video.tell(new StopMessage(), self());
        }
        // ardrone2Video does not have to be stopped

        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }

    public boolean sendData(ByteString data) {
        if (senderAddressATC != null && senderRef != null) {
            log.info("[ARDRONE2] Sending AT_COMMAND data");
            senderRef.tell(UdpMessage.send(data, senderAddressATC), getSelf());
            return true;
        } else {
            log.error("[ARDRONE2] Sending data failed (senderAddressATC or senderRef is null).");
            return false;
        }
    }

    private void sendInitNavData() {
        // Enable nav data
        // Disable bootstrap
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, ConfigKey.GEN_NAVDATA_DEMO,
                Boolean.toString(false).toUpperCase())));
        // Send ACK
        sendData(PacketCreator.createPacket(new ATCommandCONTROL(seq++)));
    }

    private void droneDiscovered(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddressATC = new InetSocketAddress(details.getIp(), DefaultPorts.AT_COMMAND.getPort());
        log.info("[ARDRONE2] Enabled SEND at protocol level. Sending port=[{}]", details.getSendingPort());
    }

    private class StopMoveMessage implements Serializable {}

    private boolean isMoveParamInRange(float moveParam) {
        return moveParam <= 1 && moveParam >= -1;
    }

    public LoggingAdapter getLog(){
        return log;
    }
}
