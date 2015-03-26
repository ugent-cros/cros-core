package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import drones.messages.DroneDiscoveredMessage;
import drones.messages.StopMessage;
import drones.models.DroneConnectionDetails;

import java.net.InetSocketAddress;

/**
 * Created by brecht on 3/25/15.
 */
public class ArDrone2Version extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final DroneConnectionDetails details;
    private final ActorRef listener;
    private final InetSocketAddress remote;

    private static final String VERSION_FILE_NAME = "version.txt";

    public ArDrone2Version(DroneConnectionDetails details, final ActorRef listener) {
        this.details = details;;
        this.listener = listener;
        this.remote = new InetSocketAddress(details.getIp(), DefaultPorts.FTP.getPort());

        final ActorRef tcp = Tcp.get(getContext().system()).manager();
        tcp.tell(TcpMessage.connect(remote), getSelf());
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Tcp.CommandFailed) {
            log.error("[ARDRONE2 VERSION] Failed ftp command. Connection timeout or buffer overflow.");
            getContext().stop(getSelf());
        } else if (msg instanceof Tcp.Connected) {
            log.info("Discovery protocol connected to [{}]", remote);

            getSender().tell(TcpMessage.register(getSelf()), getSelf());
            getContext().become(ReceiveBuilder
                    .match(Tcp.Received.class, b -> processData(b.data()))
                    .match(Tcp.CommandFailed.class, m -> commandFailed())
                    .match(StopMessage.class, m -> getContext().stop(getSelf()))
                    .match(Tcp.ConnectionClosed.class, m -> connectionClosed()).build());

            sendVersionRequest();
        } else if(msg instanceof StopMessage){
            getContext().stop(getSelf());
        }
    }

    private void sendVersionRequest() {
        String ftpVersionFileLocation = "ftp://" + remote.getAddress().getHostAddress() + ":"
                + DefaultPorts.FTP.getPort() + "/" + VERSION_FILE_NAME;
    }

    private void processData(ByteString data) {

    }

    private void commandFailed() {
        getContext().stop(getSelf());
    }

    private void connectionClosed() {
        getContext().stop(getSelf());
    }
}
