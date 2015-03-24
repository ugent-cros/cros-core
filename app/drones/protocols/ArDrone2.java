package drones.protocols;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteIterator;
import akka.util.ByteString;
import drones.commands.*;
import drones.commands.ardrone2.atcommand.*;
import drones.messages.*;
import drones.models.DroneConnectionDetails;
import drones.util.ardrone2.PacketCreator;
import drones.util.ardrone2.PacketHelper;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static drones.models.ardrone2.NavData.*;

/**
 * Created by brecht on 3/7/15.
 */
public class ArDrone2 extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;

    private final ActorRef udpManager;
    private final ActorRef listener;

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddressATC;
    private InetSocketAddress senderAddressNAV;

    // Sequence number of command
    private int seq = 0;

    // Bytes to be sent to enable navdata
    private static final byte[] TRIGGER_NAV_BYTES = {0x01, 0x00, 0x00, 0x00};

    private static final int VIDEO_PORT = 5555;
    private static final int NAV_PORT   = 5554;
    private static final int AT_PORT    = 5556;

    private static final String ARDRONE_SESSION_ID     = "d2e081a3";      // SessionID
    private static final String ARDRONE_PROFILE_ID     = "be27e2e4";      // Profile ID
    private static final String ARDRONE_APPLOCATION_ID = "d87f7e0c";  // Application ID

    private ActorRef ardrone2ResetWDG;

    public ArDrone2(DroneConnectionDetails details, final ActorRef listener) {
        this.details = details;

        this.listener = listener;

        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", details.getReceivingPort())), getSelf());
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", VIDEO_PORT)), getSelf());

        final ActorRef tcpManager = Tcp.get(getContext().system()).manager();
        tcpManager.tell(TcpMessage.connect(new InetSocketAddress("192.168.1.1", 44444)), getSelf());

        log.info("Starting ARDrone 2.0 Protocol, listening on port: [{}]", details.getReceivingPort());
    }

    @Override
    public void preStart() {
        log.info("[ARDRONE2] Starting ARDrone 2.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
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

                            // Drone commands
                            //.match(DroneCommandMessage.class, s -> dispatchCommand(s))
                    .match(InitDroneCommand.class, s -> handleInit())
                    .match(FlatTrimCommand.class, s -> handleFlatTrim())
                    .match(TakeOffCommand.class, s -> handleTakeoff())
                    .match(LandCommand.class, s -> handleLand())
                    .match(RequestStatusCommand.class, s -> handleRequestStatus())
                    .match(OutdoorCommand.class, s -> handleOutdoor(s))
                    .match(RequestSettingsCommand.class, s -> handleRequestSettings())
                    .match(MoveCommand.class, s -> handleMove(s))
                    .match(SetMaxHeightCommand.class, s -> handleSetMaxHeight(s.getMeters()))
                    .match(SetMaxTiltCommand.class, s -> handleSetMaxTilt(s.getDegrees()))
                    .matchAny(s -> {
                        log.warning("No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());

            listener.tell(new PingMessage(), getSelf());
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

    private void droneDiscovered(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddressATC = new InetSocketAddress(details.getIp(), AT_PORT);
        this.senderAddressNAV = new InetSocketAddress(details.getIp(), NAV_PORT);
        log.info("[ARDRONE2] Enabled SEND at protocol level. Sending port=[{}]", details.getSendingPort());
    }

    private void stop() {
        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }

    public boolean sendData(ByteString data) {
        if (senderAddressATC != null && senderRef != null) {
            log.info("[ARDRONE2] Sending RAW data to: [{}] - [{}]", senderAddressATC.getAddress().getHostAddress(), AT_PORT);
            senderRef.tell(UdpMessage.send(data, senderAddressATC), getSelf());
            return true;
        } else {
            log.info("[ARDRONE2] Sending data failed (senderAddressATC or senderRef is null).");
            return false;
        }
    }

    public boolean sendNavData(ByteString data) {
        if (senderAddressNAV != null && senderRef != null) {
            log.info("[ARDRONE2] Sending RAW data to: [{}] - [{}]", senderAddressNAV.getAddress().getHostAddress(), NAV_PORT);
            senderRef.tell(UdpMessage.send(data, senderAddressNAV), getSelf());
            return true;
        } else {
            log.info("[ARDRONE2] Sending data failed (senderAddressATC or senderRef is null).");
            return false;
        }
    }

    private void processRawData(ByteString data) {
        log.info("[ARDRONE2] Message received");
        byte[] received = new byte[data.length()];
        ByteIterator it = data.iterator();

        int i = 0;
        while(it.hasNext()) {
            received[i] = it.getByte();
            i++;
        }

        processData(received);
    }

    private void processData(byte[] navdata) {
        int state       = PacketHelper.getInt(navdata, NAV_STATE_OFFSET.getOffset());
        int battery     = PacketHelper.getInt(navdata, NAV_BATTERY_OFFSET.getOffset());
        float altitude  = PacketHelper.getInt(navdata, NAV_ALTITUDE_OFFSET.getOffset()) / 1000f;
        float pitch     = PacketHelper.getFloat(navdata, NAV_PITCH_OFFSET.getOffset()) / 1000f;
        float roll      = PacketHelper.getFloat(navdata, NAV_ROLL_OFFSET.getOffset()) / 1000f;
        float yaw       = PacketHelper.getFloat(navdata, NAV_YAW_OFFSET.getOffset()) / 1000f;
        float latitude  = PacketHelper.getFloat(navdata, NAV_LATITUDE_OFFSET.getOffset());
        float longitude = PacketHelper.getFloat(navdata, NAV_LONGITUDE_OFFSET.getOffset());

        // TODO nog te verwijderen
        int tmp = 0, n = 0;

        for (int i=3; i>=0; i--) {
            n <<= 8;
            tmp = navdata[NAV_BATTERY_OFFSET.getOffset() + i] & 0xFF;
            n |= tmp;
        }
        log.info("BATTERY OLD METHOD: {}", n);

        Object batteryMessage = new BatteryPercentageChangedMessage((byte) battery);
        listener.tell(batteryMessage, getSelf());

        Object altitudeMessage = new AltitudeChangedMessage(altitude);
        listener.tell(altitudeMessage, getSelf());

        Object attitudeMessage = new AttitudeChangedMessage(roll, pitch, yaw);
        listener.tell(attitudeMessage, getSelf());

        String debug = String.format(
                "[ARDRONE2] NavData received \n " +
                        "- Battery: %d \n " +
                        "- Altitude: %f", battery, altitude);
        log.info(debug);

    }

    private void handleTakeoff() {
        log.info("[ARDRONE2] TakeOff");
        sendData(PacketCreator.createTakeOffPacket(seq++));
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
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, "custom:session_id", ARDRONE_SESSION_ID)));
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, "custom:profile_id", ARDRONE_PROFILE_ID)));
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq, "custom:application_id", ARDRONE_APPLOCATION_ID)));

        sendData(PacketCreator.createPacket(new ATCommandCOMWDG(seq++)));

        // 3m max height
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, "control:altitude_max", "3000")));
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, "general:navdata_demo", "FALSE")));

        // Enable nav data
        sendNavData(ByteString.fromArray(TRIGGER_NAV_BYTES));

        ardrone2ResetWDG = getContext().actorOf(Props.create(drones.protocols.ArDrone2ResetWDG.class,
                () -> new drones.protocols.ArDrone2ResetWDG(details)));
    }

    private ATCommandCONFIGIDS getConfigIDS(int seq) {
        return new ATCommandCONFIGIDS(seq, ARDRONE_SESSION_ID, ARDRONE_PROFILE_ID, ARDRONE_APPLOCATION_ID);
    }

    private void handleRequestStatus() {
        // @TODO
        //sendData(PacketCreator.createPacket(null));
    }

    private void handleFlatTrim() {
        sendData(PacketCreator.createPacket(new ATCommandFTRIM(seq++)));
    }

    private void handleSetMaxTilt(float degrees) {
        // @TODO
        //sendData(PacketCreator.createPacket(null));
    }

    private void handleSetMaxHeight(float meters) {
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                "control:altitude_max", Integer.toString(Math.round(meters * 1000)))));
    }

    private void handleMove(MoveCommand s) {
        // Vx: pitch - front-back Must between -1..1
        // Vy: roll  - left-right Must between -1..1
        // Vr: yaw   - angular    Must between -1..1
        // Vz: gaz   - vertical   Must between -1..1
        if(isMoveParamInRange((float) s.getVx()) && isMoveParamInRange((float) s.getVy())
                && isMoveParamInRange((float) s.getVz()) && isMoveParamInRange((float) s.getVz()))
            sendData(PacketCreator.createPacket(new ATCommandPCMD(seq++, 1,
                    (float) s.getVy(), (float) s.getVx(), (float) s.getVz(), (float) s.getVr())));
    }

    private boolean isMoveParamInRange(float moveParam) {
        return moveParam <= 1 && moveParam >= -1;
    }

    /**
     * Control message (p74 manual)
     *
     * @param cmd
     */
    private void handleOutdoor(OutdoorCommand cmd) {
        sendData(PacketCreator.createPacket(getConfigIDS(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++,
                "control:outdoor", Boolean.toString(cmd.isOutdoor()).toUpperCase())));
    }

    private void handleRequestSettings() {
        // @TODO
        //sendData(PacketCreator.createPacket(null));
    }

    public LoggingAdapter getLog(){
        return log;
    }
}
