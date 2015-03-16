package drones.protocols;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.Procedure;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteIterator;
import akka.util.ByteString;
import drones.commands.*;
import drones.commands.ardrone2.atcommand.*;
import drones.messages.PingMessage;
import drones.messages.StopMessage;
import drones.models.DroneConnectionDetails;
import drones.util.ardrone2.PacketCreator;
import drones.util.ardrone2.PacketHelper;
import drones.util.ardrone3.FrameHelper;

import java.net.InetSocketAddress;

/**
 * Created by brecht on 3/7/15.
 */
public class ArDrone2 extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;

    private final ActorRef udpManager;
    private final ActorRef listener;

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;

    private int seq = 0;

    public ArDrone2(DroneConnectionDetails details, final ActorRef listener) {
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());
        this.details = details;

        this.listener = listener;

        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", details.getReceivingPort())), getSelf());
        log.debug("Starting ARDrone 2.0 Protocol, listening on port: [{}]", details.getReceivingPort());
    }

    @Override
    public void preStart() {
        log.debug("Starting ARDrone 2.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            log.debug("Socket ARDRone 2.0 bound.");

            senderRef = getSender();
            //getContext().become(ready(senderRef));

            // Setup handlers
            getContext().become(ReceiveBuilder
                    .match(Udp.Received.class, s -> processRawData(s.data()))
                    .match(Udp.Unbound.class, s -> getContext().stop(getSelf()))
                    .match(DroneConnectionDetails.class, s -> droneDiscovered(s))
                    .match(StopMessage.class, s -> stop())

                     // Drone commands
                    .match(DroneCommandMessage.class, s -> dispatchCommand(s))
                    .match(FlatTrimCommand.class, s -> handleFlatTrim())
                    .match(TakeOffCommand.class, s -> handleTakeoff())
                    .match(LandCommand.class, s -> handleLand())
                    .match(RequestStatusCommand.class, s -> handleRequestStatus())
                    .match(OutdoorCommand.class, s -> handleOutdoor(s))
                    .match(RequestSettingsCommand.class, s -> handleRequestSettings())
                    .matchAny(s -> {
                        log.warning("No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());


            listener.tell(new PingMessage(), getSelf());
        } else if (msg instanceof DroneConnectionDetails) {
            log.debug("DroneConnectionDetails received");
            droneDiscovered((DroneConnectionDetails) msg);
        } else if (msg instanceof StopMessage) {
            log.debug("Stop message received - ArDrone2 protocol");
            stop();
        } else {
            log.debug("Unhandled message received - ArDrone2 protocol");
            unhandled(msg);
        }


    }

    /*private Procedure<Object> ready(final ActorRef socket) {
        return msg -> {
            if (msg instanceof Udp.Received) {
                final Udp.Received r = (Udp.Received) msg;
                processRawData(r.data());
            } else if (msg.equals(UdpMessage.unbind())) {
                socket.tell(msg, getSelf());
            } else if (msg instanceof Udp.Unbound) {
                getContext().stop(getSelf());
            } else if (msg instanceof DroneCommandMessage) {
                dispatchCommand((DroneCommandMessage) msg);
            } else if (msg instanceof StopMessage) {
                stop();
            } else if (msg instanceof DroneConnectionDetails) {
                log.debug("DroneConnectionDetails received");
                droneDiscovered((DroneConnectionDetails) msg);
            }else {
                log.debug("Unknown message received - ArDrone2 protocol");
                unhandled(msg);
            }
        };
    }*/

    private void droneDiscovered(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());
        log.debug("Enabled SEND at protocol level. Sending port=[{}]", details.getSendingPort());
    }

    private void dispatchCommand(DroneCommandMessage msg) {
        if (msg.getMessage() instanceof TakeOffCommand) {
            log.debug("TakeOff Command Received - ArDrone2 Protocol");
            handleTakeoff();
        } else if (msg.getMessage() instanceof LandCommand) {
            log.debug("Land Command Received - ArDrone2 Protocol");
            handleLand();
        } else if (msg.getMessage() instanceof InitDroneCommand) {
            log.debug("Starting ARDrone 2.0 - ArDrone2 Protocol");
            handleInit();
        } else {
            log.warning("No handler for: [{}]", msg.getMessage().getClass().getCanonicalName());
        }
    }

    private void stop() {
        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }

    public boolean sendData(ByteString data) {
        if (senderAddress != null && senderRef != null) {
            log.debug("Sending RAW data to: [{}] - [{}]", senderAddress.getAddress().getHostAddress(), senderAddress.getPort());
            senderRef.tell(UdpMessage.send(data, senderAddress), getSelf());
            return true;
        } else {
            log.debug("Sending data failed (senderAddress or senderRef is null).");
            return false;
        }
    }

    private void processRawData(ByteString data) {
        byte[] received = new byte[data.length()];
        ByteIterator it = data.iterator();

        int i = 0;
        while(it.hasNext()) {
            received[i] = it.getByte();
            System.out.print((char) it.getByte());
            i++;
        }

        processData(received);

        log.debug("Hey, It looks like ya got a message!");
    }

    // Bytes to be sent to enable navdata
    private static final byte[] TRIGGER_NAV_BYTES = {0x01, 0x00, 0x00, 0x00};

    private static final int NAV_STATE_OFFSET   =  4;
    private static final int NAV_BATTERY_OFFSET = 24;
    private static final int NAV_PITCH_OFFSET   = 28;
    private static final int NAV_ROLL_OFFSET    = 32;
    private static final int NAV_YAW_OFFSET     = 36;
    private static final int NAV_ALTITUDE_OFFSET= 40;

    //Reuse some fields for NavData2 from Arduino sensor board with GPS/Compass/Barometer
    private static final int NAV_LATITUDE_OFFSET        = 44;
    private static final int NAV_LONGITUDE_OFFSET       = 48;
    private static final int NAV_HEADING_OFFSET         = 52;
    private static final int NAV_ALTITUDE_US_OFFSET     = 56;
    private static final int NAV_ALTITUDE_BARO_OFFSET   = 60;
    private static final int NAV_ALTITUDE_BARO_RAW_OFFSET = 64;


    private void processData(byte[] navdata) {
        int state       = PacketHelper.getInt(navdata, NAV_STATE_OFFSET);
        int battery     = PacketHelper.getInt(navdata, NAV_BATTERY_OFFSET);
        float altitude  = ((float)PacketHelper.getInt(navdata, NAV_ALTITUDE_OFFSET)) / 1000;
        float pitch     = ((float) PacketHelper.getFloat(navdata, NAV_PITCH_OFFSET)) / 1000;
        float roll      = ((float) PacketHelper.getFloat(navdata, NAV_ROLL_OFFSET)) / 1000;
        float yaw       = ((float) PacketHelper.getFloat(navdata, NAV_YAW_OFFSET)) / 1000;
        float latitude  = PacketHelper.getFloat(navdata, NAV_LATITUDE_OFFSET);
        float longitude = PacketHelper.getFloat(navdata, NAV_LONGITUDE_OFFSET);
        //PacketHelper.getFloat(navdata, NAV_HEADING_OFFSET);
        //((float)getInt(navdata, NAV_ALTITUDE_US_OFFSET)/1000);
        //PacketHelper.getFloat(navdata, NAV_ALTITUDE_BARO_OFFSET);
        //PacketHelper.getFloat(navdata, NAV_ALTITUDE_BARO_RAW_OFFSET);

    }

    private void handleTakeoff() {
        sendData(PacketCreator.createTakeOffPacket(seq++));
    }

    private void handleLand() {
        sendData(PacketCreator.createLandingPacket(seq++));
    }

    /**
     * For the init code see: https://github.com/puku0x/cvdrone/blob/master/src/ardrone/command.cpp
     */
    private void handleInit() {
        log.debug("INIT CODE ARDRONE 2");

        sendData(PacketCreator.createPacket(new ATCommandPMODE(seq++, 2))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandMISC(seq++, 2,20,2000,3000))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandFTRIM(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCOMWDG(seq++)));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, "control:altitude_max", "10000"))); // 10m max height
        sendData(PacketCreator.createPacket(new ATCommandCONFIG(seq++, "general:navdata_demo", "TRUE")));

        sendData(ByteString.fromArray(TRIGGER_NAV_BYTES));
    }

    private void handleRequestStatus() {
    }

    private void handleFlatTrim() {
    }

    private void handleOutdoor(OutdoorCommand cmd) {
    }

    private void handleRequestSettings() {
    }

    public LoggingAdapter getLog(){
        return log;
    }
}
