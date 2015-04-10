package drones.protocols.ardrone2;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteIterator;
import akka.util.ByteString;
import drones.messages.ProductVersionChangedMessage;
import drones.messages.RequestConfigMessage;
import drones.messages.StopMessage;
import drones.models.DroneConnectionDetails;

import java.net.InetSocketAddress;

/**
 * Created by brecht on 4/6/15.
 */
public class ArDrone2Config extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef listener;
    private final ActorRef parent;

    // TCP Manager
    private final ActorRef tcpManager;

    private final InetSocketAddress remote;

    public ArDrone2Config(DroneConnectionDetails details, final ActorRef listener, final ActorRef parent) {
        // ArDrone 2 Model
        this.listener = listener;
        this.parent = parent;

        this.remote = new InetSocketAddress(details.getIp(), DefaultPorts.CONTROL_PORT.getPort());

        // TCP manager
        tcpManager = Tcp.get(getContext().system()).manager();
        tcpManager.tell(TcpMessage.connect(remote), getSelf());

        log.info("[ARDRONE2CONFIG] Starting ARDrone 2.0 Config");

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

            // Send request for CONTROL commands
            parent.tell(new RequestConfigMessage(), getSelf());
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

    byte[] configDatav= new byte[10000];

    private void processData(ByteString byteData) {
        //ByteIterator it = byteData.iterator();
        //while(it.hasNext()) {
        //    //byte
        //}

        String data = byteData.decodeString("UTF-8");
        String[] configValues = data.split("\n");

        String hardwareVersion = "";
        String softwareVersion = "";

        log.info(data);

        for(String configValue: configValues) {
            String[] configPair = configValue.replaceAll("\\s+","").split("=");

            String key, value;
            if(configPair.length == 2) {
                key = configPair[0];
                value = configPair[1];
            } else {
                break;
            }

            if(key.equals(ConfigKeys.GEN_NUM_VERSION_SOFT.getKey())) {
                log.info("- Software version: {}", value);
                softwareVersion = value;
            } else if(key.equals(ConfigKeys.GEN_NUM_VERSION_MB.getKey())) {
                log.info("- Hardware version: {}", value);
                hardwareVersion = value;
                break; // @TODO temp fix
            }
        }

        Object versionMessage = new ProductVersionChangedMessage(softwareVersion, hardwareVersion);
        listener.tell(versionMessage, getSelf());

        log.info(data);
    }

    private void parseData(String data) {

    }
}
