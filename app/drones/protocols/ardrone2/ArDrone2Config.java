package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import drones.messages.StopMessage;
import drones.models.DroneConnectionDetails;

import java.net.InetSocketAddress;

/**
 * Created by brecht on 4/6/15.
 */
public class ArDrone2Config extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;
    private final ActorRef listener;
    private final ActorRef parent;
    private final ActorRef tcpManager;

    private final InetSocketAddress remote;

    private InetSocketAddress senderAddressConfig;

    public ArDrone2Config(DroneConnectionDetails details, final ActorRef listener, final ActorRef parent) {
        // ArDrone 2 Model
        this.listener = listener;
        this.parent = parent;

        this.remote = new InetSocketAddress(details.getIp(), DefaultPorts.CONTROL_PORT.getPort());
        // TCP manager
        tcpManager = Tcp.get(getContext().system()).manager();
        tcpManager.tell(TcpMessage.connect(remote), getSelf());

        log.info("[ARDRONE2] Starting ARDrone 2.0 Protocol");
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Tcp.CommandFailed) {
            log.error("[ARDRONE2CONFIG] TCP command failed");
            getContext().stop(getSelf());
        } else if (msg instanceof Tcp.Connected) {
            log.info("[ARDRONE2CONFIG] Connected to {}", remote);

            getSender().tell(TcpMessage.register(getSelf()), getSelf());
            getContext().become(ReceiveBuilder
                    .match(Tcp.Received.class, b -> processData(b.data()))
                    .match(Tcp.CommandFailed.class, m -> commandFailed())
                    .match(StopMessage.class, m -> getContext().stop(getSelf()))
                    .match(Tcp.ConnectionClosed.class, m -> connectionClosed()).build());
        } else if(msg instanceof StopMessage){
            getContext().stop(getSelf());
        }
    }

    private void connectionClosed() {
        log.info("[ARDRONE2CONFIG] TCP connection closed");
        getContext().stop(getSelf());
    }

    private void commandFailed() {
        log.error("[ARDRONE2CONFIG] TCP command failed");
        getContext().stop(getSelf());
    }

    private void processData(ByteString byteData) {
        String data = byteData.decodeString("UTF-8");

        log.info(data);
    }
}
