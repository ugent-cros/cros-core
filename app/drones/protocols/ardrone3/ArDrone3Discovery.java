package drones.protocols.ardrone3;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Inet;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.Procedure;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import drones.messages.DroneDiscoveredMessage;
import drones.messages.StopMessage;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Cedric on 3/8/2015.
 */
public class ArDrone3Discovery extends UntypedActor {

    public final static int CONNECT_TIMEOUT = 2; // already fail after 2 seconds

    private static final String DISCOVERY_MSG =
            "{ \"d2c_port\": %d,\n" +
                    "\"controller_name\": \"actor\",\n" +
                    "\"controller_type\": \"cros\" }";

    final InetSocketAddress remote;
    final ActorRef listener;
    final int commandPort;
    boolean sentResult = false;

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public ArDrone3Discovery(String ip, ActorRef listener, int commandPort) {
        this.commandPort = commandPort;
        this.remote = new InetSocketAddress(ip, 44444);
        this.listener = listener;

        final ActorRef tcp = Tcp.get(getContext().system()).manager();
        final List<Inet.SocketOption> options = new ArrayList<>();
        tcp.tell(TcpMessage.connect(remote, null, options, new FiniteDuration(CONNECT_TIMEOUT, TimeUnit.SECONDS), false), getSelf());
    }

    private void sendDiscoveryMsg(final ActorRef sender) {
        ByteStringBuilder b = new ByteStringBuilder();
        String data = String.format(DISCOVERY_MSG, commandPort);
        b.putBytes(data.getBytes(Charset.forName("UTF-8")));
        b.putByte((byte)0); //null terminated string
        sender.tell(TcpMessage.write(b.result()), getSelf());
        log.debug("Discovery message sent to drone.");
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Tcp.CommandFailed) {
            log.error("Failed discovery protocol. Connection timeout or buffer overflow.");
            listener.tell(new DroneDiscoveredMessage(DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED), getSelf());
            getContext().stop(getSelf());
        } else if (msg instanceof Tcp.Connected) {
            log.info("Discovery protocol connected to [{}]", remote);

            //TODO: add timeout for receiving data when no response
            getSender().tell(TcpMessage.register(getSelf()), getSelf());
            getContext().become(ReceiveBuilder
                    .match(Tcp.Received.class, b -> processData(b.data()))
                    .match(Tcp.CommandFailed.class, m -> commandFailed())
                    .match(StopMessage.class, m -> getContext().stop(getSelf()))
                    .match(Tcp.ConnectionClosed.class, m -> connectionClosed()).build());
            sendDiscoveryMsg(getSender());
        } else if(msg instanceof StopMessage){
            getContext().stop(getSelf());
        }
    }

    private void processData(ByteString b) throws IOException {
        String data = b.decodeString("UTF-8");

        ObjectMapper m = new ObjectMapper();
        JsonNode node = m.readTree(data);

        int status = node.path("status").intValue();
        if(status != 0){
            log.error("Drone did not acknowledge our discovery. Other controller might be in control.");
            listener.tell(new DroneDiscoveredMessage(DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED), getSelf());
        } else {
            int c2d_port = node.path("c2d_port").intValue();
            int arstream_fragment_size = node.path("arstream_fragment_size").intValue();
            int arstream_fragment_maximum_number = node.path("arstream_fragment_maximum_number").intValue();
            int arstream_max_ack_interval = node.path("arstream_max_ack_interval").intValue();
            int c2d_update_port = node.path("c2d_update_port").intValue();
            int c2d_user_port = node.path("c2d_user_port").intValue();
            log.info("Discovery succes, c2d port = [{}]", c2d_port);
            listener.tell(new DroneDiscoveredMessage(DroneDiscoveredMessage.DroneDiscoveryStatus.SUCCESS, c2d_port, commandPort,
                    arstream_fragment_size, arstream_fragment_maximum_number, arstream_max_ack_interval,
                    c2d_update_port, c2d_user_port ), getSelf());
        }
        sentResult = true;
    }

    private void connectionClosed(){
        if(!sentResult){
            log.error("Drone closed connection before result.");
            listener.tell(new DroneDiscoveredMessage(DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED), getSelf());
        }
        getContext().stop(getSelf());
    }

    private void commandFailed(){
        log.error("Failed discovery protocol after connection, TCP buffer is full.");
        listener.tell(new DroneDiscoveredMessage(DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED), getSelf());
        sentResult = true;
        getContext().stop(getSelf());
    }
}
