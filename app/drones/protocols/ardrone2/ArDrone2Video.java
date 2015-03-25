package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import drones.models.DroneConnectionDetails;

import java.net.InetSocketAddress;

/**
 * Created by brecht on 3/25/15.
 */
public class ArDrone2Video extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;
    private ActorRef listener;
    private final ActorRef udpManager;

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddressVIDEO;

    public ArDrone2Video(DroneConnectionDetails details, ActorRef listener) {
        this.details = details;
        this.listener = listener;

        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("0.0.0.0", DefaultPorts.VIDEO_DATA.getPort())), getSelf());

        this.senderAddressVIDEO = new InetSocketAddress(details.getIp(), DefaultPorts.VIDEO_DATA.getPort());
    }

    @Override
    public void onReceive(Object message) throws Exception {

    }
}
