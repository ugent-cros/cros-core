package drones.protocols.ardrone2;

import akka.actor.ActorRef;
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
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", 0)), getSelf());

        this.senderAddressNAV = new InetSocketAddress(details.getIp(), DefaultPorts.NAV_DATA.getPort());
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Udp.Bound) {
            log.info("[ARDRONE2NAVDATA] Socket ARDRone 2.0 bound.");

            senderRef = getSender();

            // Setup handlers
            getContext().become(ReceiveBuilder
                    .match(Udp.Received.class, s -> processRawData(s.data()))
                    .match(Udp.Unbound.class, s -> getContext().stop(getSelf()))
                    .match(StopMessage.class, s -> stop())
                    .matchAny(s -> {
                        log.info("[ARDRONE2NAVDATA] No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());

            // Enable nav data
            sendNavData(ByteString.fromArray(TRIGGER_NAV_BYTES));
            parent.tell(new InitCompletedMessage(), getSelf());
        } else {
            log.info(msg.toString());
            log.info("[ARDRONE2NAVDATA] Unhandled message received");
            unhandled(msg);
        }
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

    private void processRawData(ByteString data) {
        log.info("[ARDRONE2NAVDATA] Message received");
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
        if(navdata.length >= 100 ) { // Otherwise this will crash
            int header = PacketHelper.getInt(navdata, NAV_HEADER_OFFSET.getOffset());
            if(header != HEADER_VALUE) {
                log.info("Wrong header received");
                return;
            }

            attitudeChanged(navdata);
            speedChanged(navdata);
            altitudeChanged(navdata);
            batteryChanged(navdata);
            flyingStateChanged(navdata);
            alertStateChanged(navdata);
            positionChanged(navdata);
        } else {
            log.info("Packet doesn't contain data");
        }
    }

    private void attitudeChanged(byte[] navdata) {
        float pitch = PacketHelper.getFloat(navdata, NAV_PITCH_OFFSET.getOffset()) / 1000f;
        float roll = PacketHelper.getFloat(navdata, NAV_ROLL_OFFSET.getOffset()) / 1000f;
        float yaw = PacketHelper.getFloat(navdata, NAV_YAW_OFFSET.getOffset()) / 1000f;

        Object attitudeMessage = new AttitudeChangedMessage(roll, pitch, yaw);
        listener.tell(attitudeMessage, getSelf());
    }

    private void speedChanged(byte[] navdata) {
        float vx = PacketHelper.getFloat(navdata, NAV_VX_OFFSET.getOffset());
        float vy = PacketHelper.getFloat(navdata, NAV_VY_OFFSET.getOffset());
        float vz = PacketHelper.getFloat(navdata, NAV_VZ_OFFSET.getOffset());

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
            listener.tell(alertMessage, getSelf());;
        }

        if ((state & (1 << State.EMERGENCY.getOffset())) == 1) {
            Object alertMessage = new AlertStateChangedMessage(AlertState.USER_EMERGENCY);
            listener.tell(alertMessage, getSelf());
        }
    }

    private void positionChanged(byte[] navdata) {
        float latitude  = PacketHelper.getFloat(navdata, NAV_LATITUDE_OFFSET.getOffset());
        float longitude = PacketHelper.getFloat(navdata, NAV_LONGITUDE_OFFSET.getOffset());
        float altitude = PacketHelper.getInt(navdata, NAV_ALTITUDE_OFFSET.getOffset()) / 1000f;

        Object locationMessage = new LocationChangedMessage(longitude, latitude, altitude);
        listener.tell(locationMessage, getSelf());
    }

    private FlyingState parseCtrlState(int state) {
        switch(state) {
            case 2:
                log.info("Landed");
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
}
