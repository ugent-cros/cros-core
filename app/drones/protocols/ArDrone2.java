package drones.protocols;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.Procedure;
import akka.util.ByteString;
import drones.commands.EmergencyCommand;
import drones.commands.LandCommand;
import drones.commands.TakeOffCommand;
import drones.models.DroneConnectionDetails;
import drones.util.ardrone2.PacketCreator;

import java.net.InetSocketAddress;

// @TODO make abstract class (WiFi protocol class or something like that), ArDrone2 & 3 will extend it
/**
 * Created by brecht on 3/7/15.
 */
public class ArDrone2 extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;
    private ActorRef connectionMgrRef;

    private ByteString recvBuffer;

    private final ActorRef listener; //to respond messages to

    private int seq = 0;

    public ArDrone2(DroneConnectionDetails details, final ActorRef listener) {
        this.details = details;
        this.listener = listener;

        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());

        //  initHandlers();

        final ActorRef mgr = Udp.get(getContext().system()).getManager();
        mgr.tell(
                UdpMessage.bind(getSelf(), new InetSocketAddress("localhost", details.getReceivingPort())),
                getSelf());
    }



    @Override
    public void preStart() {
        log.info("Starting ARDrone 2.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            this.connectionMgrRef = getSender();
            getContext().become(ready(connectionMgrRef));

            log.debug("Socket ARDRone 2.0 bound.");
        } else unhandled(msg);
    }

    public void sendData(ByteString data) {
        connectionMgrRef.tell(UdpMessage.send(data, senderAddress), getSelf());
    }


    private Procedure<Object> ready(final ActorRef socket) {
        return msg -> {
            if (msg instanceof Udp.Received) {
                final Udp.Received r = (Udp.Received) msg;
                //socket.tell(UdpMessage.send(r.data(), r.sender()), getSelf());
                //processRawData(r.data());
            } else if (msg.equals(UdpMessage.unbind())) {
                socket.tell(msg, getSelf());
            } else if (msg instanceof Udp.Unbound) {
                getContext().stop(getSelf());

            } else unhandled(msg);
        };
    }

    private void handle(TakeOffCommand cmd) {
        sendData(PacketCreator.createTakeOffPacket());
    }

    private void handle(LandCommand cmd) {
        sendData(PacketCreator.createLandingPacket());
    }

    private void handle(EmergencyCommand cmd) {
        sendData(PacketCreator.createEmergencyPacket());
    }

    public LoggingAdapter getLog(){
        return log;
    }
}
