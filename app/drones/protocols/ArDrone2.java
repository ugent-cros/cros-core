package drones.protocols;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.Procedure;
import akka.util.ByteString;
import drones.models.ATCommand.ATCommand;
import drones.models.DroneConnectionDetails;

import java.net.InetSocketAddress;

// @TODO make abstract class, ArDrone2 & 3 will extend it
/**
 * Created by brecht on 3/7/15.
 */
public class ArDrone2 extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;
    private ActorRef connectionMgrRef;

    private int seq = 0;

    public ArDrone2(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());

        final ActorRef mgr = Udp.get(getContext().system()).getManager();
        mgr.tell(
                UdpMessage.bind(getSelf(), new InetSocketAddress("localhost", details.getReceivingPort())),
                getSelf());
    }



    @Override
    public void preStart() {
        log.info("Starting ARDrone 2.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());

        // @TODO write startup code (https://projects.ardrone.org/attachments/318/ARDrone.java)
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            this.connectionMgrRef = getSender();
            getContext().become(ready(connectionMgrRef));

            log.debug("Socket ARDRone 2.0 bound.");
        } else unhandled(msg);
    }

    public void sendCommand(ATCommand command) {
        seq++;
        command.setSeq(seq);

        connectionMgrRef.tell(UdpMessage.send(ByteString.fromString(command.toString()), senderAddress), getSelf());
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


    public LoggingAdapter getLog(){
        return log;
    }
}
