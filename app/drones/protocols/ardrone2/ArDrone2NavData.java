package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteIterator;
import akka.util.ByteString;
import drones.messages.*;
import drones.models.AlertState;
import drones.models.DroneConnectionDetails;
import drones.models.FlyingState;
import drones.util.ardrone2.PacketHelper;
import java.net.InetSocketAddress;

import static drones.models.ardrone2.NavData.*;

/**
 * Created by brecht on 3/25/15.
 */
public class ArDrone2NavData extends UntypedActor {

    // Bytes to be sent to enable navdata
    private static final byte[] TRIGGER_NAV_BYTES = {0x01, 0x00, 0x00, 0x00};
    private static final int HEADER_VALUE = 0x55667788;
    private static final int MIN_SIZE = 100;

    // Percentage when battery level is low
    private static final int BATTERY_LEVEL_LOW = 30;

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;
    private final ActorRef listener;
    private final ActorRef parent;
    private final ActorRef udpManager;

    private InetSocketAddress senderAddressNAV;

    public ArDrone2NavData(DroneConnectionDetails details, ActorRef listener, ActorRef parent) {
        this.listener = listener;
        this.parent = parent;

        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress(0)), getSelf());

        this.senderAddressNAV = new InetSocketAddress(details.getIp(), DefaultPorts.NAV_DATA.getPort());
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Udp.Bound) {
            log.info("[ARDRONE2NAVDATA] Socket ARDRone 2.0 bound.");

            // Setup handlers
            getContext().become(ReceiveBuilder
                    .match(Udp.Received.class, s -> processRawData(s.data()))
                    .match(Udp.Unbound.class, s -> getContext().stop(getSelf()))
                    .match(Udp.SimpleSenderReady.class, s -> senderRef = sender())
                    .match(StopMessage.class, s -> stop())
                    .matchAny(s -> {
                        log.info("[ARDRONE2NAVDATA] No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());

            // Enable nav data
            sendNavData(ByteString.fromArray(TRIGGER_NAV_BYTES));
            parent.tell(new InitNavDataMessage(), getSelf());
        } else if(msg instanceof Udp.SimpleSenderReady){
            senderRef = sender();
        } else {
            log.info(msg.toString());
            log.info("[ARDRONE2NAVDATA] Unhandled message received");
            unhandled(msg);
        }
    }

    @Override
    public void aroundPostStop() {
        super.aroundPostStop();
        if(senderRef != null){
            senderRef.tell(new PoisonPill(){}, self()); // stop the sender
        }
    }

    private void processRawData(ByteString data) {
        log.debug("[ARDRONE2NAVDATA] Message received");
        byte[] received = new byte[data.length()];
        ByteIterator it = data.iterator();

        int i = 0;
        while(it.hasNext()) {
            received[i] = it.getByte();
            i++;
        }

        processData(received);
    }

    public boolean sendNavData(ByteString data) {
        if (senderAddressNAV != null && senderRef != null) {
            log.info("[ARDRONE2NAVDATA] Sending NAV INIT data");
            senderRef.tell(UdpMessage.send(data, senderAddressNAV), getSelf());
            return true;
        } else {
            log.info("[ARDRONE2NAVDATA] Sending data failed (senderAddressATC or senderRef is null).");
            return false;
        }
    }

    private void stop() {
        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }


    private void processData(byte[] navdata) {
        if(navdata.length >= MIN_SIZE) { // Otherwise this will crash
            int offset = 0;

            int header = PacketHelper.getInt(navdata, offset);
            if(header != HEADER_VALUE) {
                log.warning("Wrong header received");
                return;
            }
            offset += 4;

            int state = PacketHelper.getInt(navdata, offset);
            offset += 12;

            // Parse state
            while(offset < navdata.length) {
                int optionTag = PacketHelper.getShort(navdata, offset);
                offset += 2;
                int optionLen = PacketHelper.getShort(navdata, offset);
                offset += 2;

                if(optionLen == 0) {
                    log.error("Zero length option with tag: {}", optionTag);
                }

                if(optionTag == NavDataTag.DEMO_TAG.getTag()) {
                    parseDemoData(navdata);
                } else if(optionTag == NavDataTag.GPS_TAG.getTag()) {
                    parseGPSData(navdata, offset);
                }

                offset += optionLen - 4;

            }
        } else {
            log.info("Packet doesn't contain data");
            parent.tell(new InitNavDataMessage(), getSelf());
        }
    }

    private void parseDemoData(byte[] navdata) {
        attitudeChanged(navdata);
        speedChanged(navdata);
        altitudeChanged(navdata);
        batteryChanged(navdata);
        flyingStateChanged(navdata);
        alertStateChanged(navdata);
    }

    // @TODO to be tested when GPS module arrives
    private void parseGPSData(byte[] navdata, int offset) {
        int offsetTemp = offset;

        double latitude = PacketHelper.getDouble(navdata, offsetTemp);
        offsetTemp += 8;
        double longitude = PacketHelper.getDouble(navdata, offsetTemp);
        offsetTemp += 8;
        double elevation = PacketHelper.getDouble(navdata, offsetTemp);
        offsetTemp += 16; // Elevation and hdop
        int dataAvailable = PacketHelper.getInt(navdata, offsetTemp);

        boolean gpsAvailable = dataAvailable == 1;
        Object gpsFixMessage = new GPSFixChangedMessage(gpsAvailable);
        listener.tell(gpsFixMessage, getSelf());

        log.info("GPS values: [Lat: {}] [Lon: {}] [ALT: {}], available: {}", latitude, longitude, elevation, gpsAvailable); // @TODO remove
        Object locationMessage = new LocationChangedMessage(longitude, latitude, elevation);
        listener.tell(locationMessage, getSelf());
    }

    private void attitudeChanged(byte[] navdata) {
        float pitch = (float) Math.toRadians(PacketHelper.getFloat(navdata, NAV_PITCH_OFFSET.getOffset()) / 1000);
        float roll = (float) Math.toRadians(PacketHelper.getFloat(navdata, NAV_ROLL_OFFSET.getOffset()) / 1000);
        float yaw=(float) Math.toRadians(PacketHelper.getFloat(navdata, NAV_YAW_OFFSET.getOffset()) / 1000);

        Object attitudeMessage = new RotationChangedMessage(roll, pitch, yaw);
        listener.tell(attitudeMessage, getSelf());
    }

    private void speedChanged(byte[] navdata) {
        float vx = PacketHelper.getFloat(navdata, NAV_VX_OFFSET.getOffset()) / 1000f;
        float vy = PacketHelper.getFloat(navdata, NAV_VY_OFFSET.getOffset()) / 1000f;
        float vz = PacketHelper.getFloat(navdata, NAV_VZ_OFFSET.getOffset()) / 1000f;

        Object speedMessage = new SpeedChangedMessage(vx, vy, vz);
        listener.tell(speedMessage, getSelf());
    }

    private void altitudeChanged(byte[] navdata) {
        float altitude = PacketHelper.getInt(navdata, NAV_ALTITUDE_OFFSET.getOffset()) / 1000f;

        Object altitudeMessage = new AltitudeChangedMessage(altitude);
        listener.tell(altitudeMessage, getSelf());
    }

    private void batteryChanged(byte[] navdata) {
        int battery = PacketHelper.getInt(navdata, NAV_BATTERY_OFFSET.getOffset());

        Object batteryMessage = new BatteryPercentageChangedMessage((byte) battery);
        listener.tell(batteryMessage, getSelf());

        if(battery < BATTERY_LEVEL_LOW) {
            Object alertMessage = new AlertStateChangedMessage(AlertState.BATTERY_LOW);
            listener.tell(alertMessage, getSelf());
        }
    }

    private void flyingStateChanged(byte[] navdata) {
        int state = PacketHelper.getInt(navdata, NAV_STATE_OFFSET.getOffset());
        int ctrl_state = PacketHelper.getInt(navdata, NAV_CTRL_STATE_OFFSET.getOffset()) >> 16;

        if ((state & (1 << State.EMERGENCY.getOffset())) == 0) {
            Object stateMessage = new FlyingStateChangedMessage(parseCtrlState(ctrl_state));
            listener.tell(stateMessage, getSelf());
        } else {
            Object stateMessage = new FlyingStateChangedMessage(FlyingState.EMERGENCY);
            listener.tell(stateMessage, getSelf());
        }
    }

    private void alertStateChanged(byte[] navdata) {
        int state = PacketHelper.getInt(navdata, NAV_STATE_OFFSET.getOffset());

        if ((state & (1 << State.BATTERY_TOO_LOW.getOffset())) == 1) {
            Object alertMessage = new AlertStateChangedMessage(AlertState.BATTERY_CRITICAL);
            listener.tell(alertMessage, getSelf());
        }

        if ((state & (1 << State.EMERGENCY.getOffset())) == 1) {
            Object alertMessage = new AlertStateChangedMessage(AlertState.USER_EMERGENCY);
            listener.tell(alertMessage, getSelf());
        }
    }

    private FlyingState parseCtrlState(int state) {
        switch(state) {
            case 2:
                return FlyingState.LANDED;
            case 3:
                log.info("Flying");
                return FlyingState.FLYING;
            case 4:
                log.info("Hovering");
                return FlyingState.HOVERING;
            case 6:
                log.info("Taking off");
                return FlyingState.TAKINGOFF;
            case 8:
                log.info("Landing");
                return FlyingState.LANDING;
            default:
                log.info("Unknown state discovered");
                return null;
        }
    }

    private enum State {
        BATTERY_TOO_LOW(15),
        EMERGENCY(31);

        private final int offset;

        private State(int offset) {
            this.offset = offset;
        }

        private int getOffset() {
            return offset;
        }
    }

    public enum NavDataTag {
        DEMO_TAG(0),
        TIME_TAG(1),
        RAW_MEASURES_TAG(2),
        PHYS_MEASURES_TAG(3),
        GYROS_OFFSETS_TAG(4),
        EULER_ANGLES_TAG(5),
        REFERENCES_TAG(6),
        TRIMS_TAG(7),
        RC_REFERENCES_TAG(8),
        PWM_TAG(9),
        ALTITUDE_TAG(10),
        VISION_RAW_TAG(11),
        VISION_OF_TAG(12),
        VISION_TAG(13),
        VISION_PERF_TAG(14),
        TRACKERS_SEND_TAG(15),
        VISION_DETECT_TAG(16),
        WATCHDOG_TAG(17),
        IPHONE_ANGLES_TAG(18),
        ADC_DATA_FRAME_TAG(18),
        VIDEO_STREAM_TAG(19),
        GAME_TAG(20),             // AR.Drone 1.7.4
        PRESSURE_RAW_TAG(21),     // AR.Drone 2.0
        MAGNETO_TAG(22),          // AR.Drone 2.0
        WIND_TAG(23),             // AR.Drone 2.0
        KALMAN_PRESSURE_TAG(24),  // AR.Drone 2.0
        HDVIDEO_STREAM_TAG(25),   // AR.Drone 2.0
        WIFI_TAG(26),             // AR.Drone 2.0
        ZIMMU3000_TAG(27),        // AR.Drone 2.0
        GPS_TAG(27),              // AR.Drone 2.4.1
        CKS_TAG(0xFFFF);
        
        private final int tag;

        private NavDataTag(int tag) {
            this.tag = tag;
        }

        public int getTag() {
            return tag;
        }
    }
}
