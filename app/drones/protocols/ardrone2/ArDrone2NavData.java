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
import drones.commands.ardrone2.atcommand.ATCommandCONFIG;
import drones.commands.ardrone2.atcommand.ATCommandCONFIGIDS;
import drones.commands.ardrone2.atcommand.ATCommandCONTROL;
import drones.messages.*;
import drones.models.DroneConnectionDetails;
import drones.models.FlyingState;
import drones.util.ardrone2.PacketCreator;
import drones.util.ardrone2.PacketHelper;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static drones.models.ardrone2.NavData.*;
import static drones.models.ardrone2.NavData.NAV_BATTERY_OFFSET;
import static drones.models.ardrone2.NavData.NAV_LONGITUDE_OFFSET;

/**
 * Created by brecht on 3/25/15.
 */
public class ArDrone2NavData extends UntypedActor {

    // Bytes to be sent to enable navdata
    private static final byte[] TRIGGER_NAV_BYTES = {0x01, 0x00, 0x00, 0x00};

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;
    private final ActorRef listener;
    private final ActorRef parent;
    private final ActorRef udpManager;

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddressNAV;
    private InetSocketAddress senderAddressATC;

    public ArDrone2NavData(DroneConnectionDetails details, ActorRef listener, ActorRef parent) {
        this.details = details;
        this.listener = listener;
        this.parent = parent;

        udpManager = Udp.get(getContext().system()).getManager();
        //udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", details.getReceivingPortNAV())), getSelf());
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", DefaultPorts.NAV_DATA.getPort())), getSelf());

        //this.senderAddressNAV = new InetSocketAddress(details.getIp(), details.getSendingPortNAV);
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
;
        processData(received); //@TODO
    }

    private void processData(byte[] navdata) {
        if(navdata.length >= 100 ) { // Otherwise this will crash
            int state = PacketHelper.getInt(navdata, NAV_STATE_OFFSET.getOffset());
            int battery = PacketHelper.getInt(navdata, NAV_BATTERY_OFFSET.getOffset());
            float altitude = PacketHelper.getInt(navdata, NAV_ALTITUDE_OFFSET.getOffset()) / 1000f;
            float pitch = PacketHelper.getFloat(navdata, NAV_PITCH_OFFSET.getOffset()) / 1000f;
            float roll = PacketHelper.getFloat(navdata, NAV_ROLL_OFFSET.getOffset()) / 1000f;
            float yaw = PacketHelper.getFloat(navdata, NAV_YAW_OFFSET.getOffset()) / 1000f;
            //float latitude  = PacketHelper.getFloat(navdata, NAV_LATITUDE_OFFSET.getOffset());
            //float longitude = PacketHelper.getFloat(navdata, NAV_LONGITUDE_OFFSET.getOffset());

            Object stateMessage;
            if ((state & 1) == 0) {
                stateMessage = new FlyingStateChangedMessage(FlyingState.LANDED);
                log.info("Landed");
            } else {
                stateMessage = new FlyingStateChangedMessage(FlyingState.FLYING);
                log.info("Flying");
            }
            listener.tell(stateMessage, getSelf());

            Object batteryMessage = new BatteryPercentageChangedMessage((byte) battery);
            listener.tell(batteryMessage, getSelf());

            Object altitudeMessage = new AltitudeChangedMessage(altitude);
            listener.tell(altitudeMessage, getSelf());

            Object attitudeMessage = new AttitudeChangedMessage(roll, pitch, yaw);
            listener.tell(attitudeMessage, getSelf());
        }
    }
}
