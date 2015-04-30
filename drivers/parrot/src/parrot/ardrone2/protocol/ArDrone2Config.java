package parrot.ardrone2.protocol;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import parrot.ardrone2.util.ConfigKey;
import parrot.ardrone2.util.DefaultPorts;
import parrot.shared.models.DroneConnectionDetails;
import droneapi.messages.ProductVersionChangedMessage;
import parrot.messages.RequestConfigMessage;
import droneapi.messages.StopMessage;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

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

    // Starting key-pair value for config file
    private static final String CONFIG_START_VALUE = "general:num_version_config=1\n";

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

    private String data = "";

    private void processData(ByteString byteData) {
        String newData = byteData.decodeString("UTF-8");
        if(newData.toLowerCase().contains(CONFIG_START_VALUE)) {
            data = newData;
        } else {
            data += newData;
        }

        if(data.endsWith("\0")) {
            parseData(data);
        }
    }

    private void parseData(String data) {
        String hardwareVersion = "";
        String softwareVersion = "";

        List<String> configs = Arrays.asList(data.split("\n"));
        for(String configValue : configs) {
            String[] configPair = configValue.replaceAll("\\s+","").split("=");
            String key, value;

            if(configPair.length == 2) {
                key = configPair[0];
                value = configPair[1];

                ConfigKey configKey = getConfigValue(key);
                if(configKey != null) {
                    switch (configKey) {
                        case GEN_NUM_VERSION_SOFT:
                            softwareVersion = value;
                            break;
                        case GEN_NUM_VERSION_MB:
                            hardwareVersion = value;
                            break;
                        default:
                            break;
                    }
                }
            } else {
                log.error("Error in parsing config file");
            }

        }

        Object versionMessage = new ProductVersionChangedMessage(softwareVersion, hardwareVersion);
        listener.tell(versionMessage, getSelf());
    }

    private ConfigKey getConfigValue(String configValue) {
        for(ConfigKey key : ConfigKey.values()) {
            if(key.getKey().equalsIgnoreCase(configValue)) {
                return key;
            }
        }

        return null;
    }
}
