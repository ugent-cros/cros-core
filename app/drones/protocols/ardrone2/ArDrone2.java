package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import drones.commands.*;
import drones.commands.ardrone2.atcommand.*;
import drones.messages.*;
import drones.models.DroneConnectionDetails;
import drones.util.ardrone2.PacketCreator;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by brecht on 3/7/15.
 */
public class ArDrone2 extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;

    private final ActorRef udpManager;
    private final ActorRef listener;

    // UDP connection details
    private DroneConnectionDetails details;
    private InetSocketAddress senderAddressATC;

    // Sequence number of command
    private int seq = 0;

    // Session IDs
    private static final String ARDRONE_SESSION_ID     = "d2e081a3";      // SessionID
    private static final String ARDRONE_PROFILE_ID     = "be27e2e4";      // Profile ID
    private static final String ARDRONE_APPLOCATION_ID = "d87f7e0c";  // Application ID

    // Watchdog reset actor
    private ActorRef ardrone2ResetWDG;
    private ActorRef ardrone2NavData;

    public ArDrone2(DroneConnectionDetails details, final ActorRef listener) {
        // Connection details
        this.details = details;
        // ArDrone 2 Model
        this.listener = listener;
        // UPD manager
        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", DefaultPorts.AT_COMMAND.getPort())), getSelf());

        log.info("[ARDRONE2] Starting ARDrone 2.0 Protocol");
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            log.info("[ARDRONE2] Socket ARDRone 2.0 bound.");

            senderRef = getSender();
            // Setup handlers
            getContext().become(ReceiveBuilder
                    .match(Udp.Received.class, s -> processRawData(s.data()))
                    .match(Udp.Unbound.class, s -> getContext().stop(getSelf()))
                    .match(DroneConnectionDetails.class, s -> droneDiscovered(s))
                    .match(StopMessage.class, s -> stop())
                    .match(InitCompletedMessage.class, s -> sendInitNavData())
                    // Drone commands
                    .match(InitDroneCommand.class, s -> handleInit())
                    .match(CalibrateCommand.class, s -> handleCalibrate())
                    .match(ResetCommand.class, s -> handleReset())
                    .match(FlatTrimCommand.class, s -> handleFlatTrim())
                    .match(TakeOffCommand.class, s -> handleTakeoff())
                    .match(LandCommand.class, s -> handleLand())
                    .match(SetOutdoorCommand.class, s -> handleOutdoor(s))
                    .match(SetHullCommand.class, s -> setHull(s.hasHull()))
                    .match(MoveCommand.class, s -> handleMove(s))
                    .match(SetMaxHeightCommand.class, s -> handleSetMaxHeight(s.getMeters()))
                    .match(StopMoveMessage.class, s -> handleStopMove())
                    .matchAny(s -> {
                        log.warning("[ARDRONE2] No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());

            listener.tell(new InitCompletedMessage(), getSelf());
        } else if (msg instanceof DroneConnectionDetails) {
            log.info("[ARDRONE2] DroneConnectionDetails received");
            droneDiscovered((DroneConnectionDetails) msg);
        } else if (msg instanceof StopMessage) {
            log.info("[ARDRONE2] Stop message received - ArDrone2 protocol");
            stop();
        } else {
            log.info("[ARDRONE2] Unhandled message received - ArDrone2 protocol");
            unhandled(msg);
        }
    }

    private void processRawData(ByteString data) {
        log.info("[ARDRONE2] Unexpected data received");
    }

    private void handleStopMove() {
        sendData(PacketCreator.createPacket(new ATCommandPCMD(seq++, 0, -0f, -0f, 0f, -0f)));
    }

    private void handleCalibrate() {
        log.info("[ARDRONE2] Calibrate");
        sendData(PacketCreator.createPacket(new ATCommandCALIB(seq++)));
    }

    private void handleReset() {
        log.info("[ARDRONE2] Reset");
        sendData(PacketCreator.createEmergencyPacket(seq++));
    }

    private void droneDiscovered(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddressATC = new InetSocketAddress(details.getIp(), DefaultPorts.AT_COMMAND.getPort()); // @TODO vervangen door conn details
        log.info("[ARDRONE2] Enabled SEND at protocol level. Sending port=[{}]", details.getSendingPort());
    }

    private void stop() {
        ardrone2ResetWDG.tell(new StopMessage(), self());
        ardrone2NavData.tell(new StopMessage(), self());

        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }

    public boolean sendData(ByteString data) {
        if (senderAddressATC != null && senderRef != null) {
            log.info("[ARDRONE2] Sending AT_COMMAND data");
            senderRef.tell(UdpMessage.send(data, senderAddressATC), getSelf());
            return true;
        } else {
            log.info("[ARDRONE2] Sending data failed (senderAddressATC or senderRef is null).");
            return false;
        }
    }

    private void handleTakeoff() {
        log.info("[ARDRONE2] TakeOff");
        sendData(PacketCreator.createTakeOffPacket(seq++));

        Akka.system().scheduler().scheduleOnce(Duration.create(250, TimeUnit.MILLISECONDS),
                getSelf(), new CalibrateCommand(), Akka.system().dispatcher(), null);
    }

    private void handleLand() {
        log.info("[ARDRONE2] Land");
        sendData(PacketCreator.createLandingPacket(seq++));
    }

    /**
     * For the init code see: https://github.com/puku0x/cvdrone/blob/master/src/ardrone/command.cpp
     */
    private void handleInit() {
        log.info("[ARDRONE2] Initializing ARDrone 2.0");

        sendData(PacketCreator.createPacket(new ATCommandPMODE(seq++, 2))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandMISC(seq++, 2,20,2000,3000))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandFTRIM(seq++)));

        // Set the sessions
        // Set the configuration IDs
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, "custom:session_id", ARDRONE_SESSION_ID)));
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, "custom:profile_id", ARDRONE_PROFILE_ID)));
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, "custom:application_id", ARDRONE_APPLOCATION_ID)));

        sendData(PacketCreator.createPacket(new ATCommandCOMWDG(seq++)));

        // 3m max height
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, "control:altitude_max", "3000")));

        // Create watchdog actor
        ardrone2ResetWDG = getContext().actorOf(Props.create(ArDrone2ResetWDG.class,
                () -> new ArDrone2ResetWDG(details)));

        // Create nav data actor
        ardrone2NavData = getContext().actorOf(Props.create(ArDrone2NavData.class,
                () -> new ArDrone2NavData(details, listener, getSelf())));
    }

    private void sendInitNavData() {
        log.info("[ARDRONE2] Init completed");
        // Enable nav data
        // Disable bootstrap
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, "general:navdata_demo", "TRUE")));
        // Send ACK
        sendData(PacketCreator.createPacket(new ATCommandCONTROL(seq++)));
    }

    private ATCommandCONFIGIDS createConfigIDS(int seq) {
        return new ATCommandCONFIGIDS(seq, ARDRONE_SESSION_ID, ARDRONE_PROFILE_ID, ARDRONE_APPLOCATION_ID);
    }

    private void handleFlatTrim() {
        sendData(PacketCreator.createPacket(new ATCommandFTRIM(seq++)));
    }

    private void setHull(boolean hull) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                "control:flight_without_shell", Boolean.toString(!hull).toUpperCase())));
    }

    private void handleSetMaxHeight(float meters) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                "control:altitude_max", Integer.toString(Math.round(meters * 1000)))));
    }

    private void handleMove(MoveCommand s) {
        if(isMoveParamInRange((float) s.getVx()) && isMoveParamInRange((float) s.getVy())
                && isMoveParamInRange((float) s.getVz()) && isMoveParamInRange((float) s.getVr())) {

            float[] v = {-0.2f * (float) s.getVy(), -0.2f * (float) s.getVx(),
                    1.0f * (float) s.getVz(), -0.5f * (float) s.getVr()};
            boolean mode = Math.abs(v[0]) > 0.0 || Math.abs(v[1]) > 0.0;

            // Nomarization (-1.0 to +1.0)
            for (int i = 0; i < 4; i++) {
                if (Math.abs(v[i]) > 1.0) {
                    v[i] /= Math.abs(v[i]);
                }
            }

            sendData(PacketCreator.createPacket(new ATCommandPCMD(seq++, mode ? 1 : 0,
                    v[1], v[0], v[2], v[3])));

            log.info("[ARDRONE2 MOVE] y: {}, x: {}, z: {}, r: {}", v[0], v[1], v[2], v[3]);

            Akka.system().scheduler().scheduleOnce(Duration.create(1000, TimeUnit.MILLISECONDS),
                    getSelf(), new StopMoveMessage(), Akka.system().dispatcher(), null);
        }
    }

    private class StopMoveMessage implements Serializable {}

    private boolean isMoveParamInRange(float moveParam) {
        return moveParam <= 1 && moveParam >= -1;
    }

    private void handleOutdoor(SetOutdoorCommand cmd) {
        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                "control:outdoor", Boolean.toString(cmd.isOutdoor()).toUpperCase())));

        sendData(PacketCreator.createPacket(createConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                "control:flight_without_shell", Boolean.toString(cmd.isOutdoor()).toUpperCase())));
    }

    public LoggingAdapter getLog(){
        return log;
    }
}
