package parrot.ardrone3;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import parrot.ardrone3.protocol.ArDrone3;
import parrot.ardrone3.protocol.ArDrone3Discovery;
import parrot.messages.DroneDiscoveredMessage;
import parrot.shared.commands.*;
import parrot.shared.models.DroneConnectionDetails;
import droneapi.messages.StopMessage;
import droneapi.model.DroneException;
import droneapi.model.NavigatedDroneActor;
import droneapi.model.properties.ConnectionStatus;
import droneapi.model.properties.FlipType;
import droneapi.model.properties.Location;
import droneapi.navigator.LocationNavigator;
import parrot.shared.commands.MoveCommand;
import org.joda.time.DateTime;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by Cedric on 3/8/2015.
 */
public class Bebop extends NavigatedDroneActor {

    private static final int RECONNECT_MAX_BACKOFF = 32; // max delay before reconnect

    private ActorRef protocol;
    private ActorRef discoveryProtocol;

    private final String ip;
    private final boolean indoor;
    private final boolean hull;

    private final Object lock = new Object();

    private Promise<Void> initPromise;
    private ConnectionStatus connectionStatus;

    private int reconnectBackoff;

    private int d2cPort;

    //TODO: use configuration class to pass here
    public Bebop(int d2cPort, String ip, boolean indoor, boolean hull) {
        this.hull = hull;
        this.d2cPort = d2cPort;
        this.ip = ip;
        this.indoor = indoor;
        this.connectionStatus = ConnectionStatus.NOT_INITIALIZED;
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(DroneDiscoveredMessage.class, this::handleDroneDiscoveryResponse).
                match(String.class, s -> "reconnect".equals(s), s -> handleReconnect());
    }

    @Override
    protected LocationNavigator createNavigator(Location currentLocation, Location goal) {
        return new LocationNavigator(currentLocation, goal,
                4f, 60f, 0.4f); // Bebop parameters
    }

    private void handleDroneDiscoveryResponse(DroneDiscoveredMessage s) {
        switch(s.getStatus()){
            case FAILED:
                switch(connectionStatus){
                    case CONNECTING:
                        if (initPromise != null) {
                            initPromise.failure(new DroneException("Failed to get drone discovery response."));
                            initPromise = null;
                        }
                        break;
                    case RECONNECTING:
                        // schedule new reconnect attempt, exponentially backoff
                        reconnectBackoff = Math.min(RECONNECT_MAX_BACKOFF, reconnectBackoff * 2);

                        getContext().system().scheduler().scheduleOnce(
                                Duration.create(reconnectBackoff, TimeUnit.SECONDS),
                                self(), "reconnect", getContext().dispatcher(), null);
                        break;
                }
                break;
            case SUCCESS:
                reconnectBackoff = ArDrone3Discovery.CONNECT_TIMEOUT + 1; //reset reconnection backoff
                if(initPromise != null){
                    initPromise.success(null);
                    initPromise = null;
                }
                setupDrone(s, connectionStatus == ConnectionStatus.RECONNECTING);
                connectionStatus = ConnectionStatus.CONNECTED;
                setConnectionStatus(true);
                break;

            default:
                log.warning("Invalid drone discovery response status.");
                break;
        }
    }

    private <T extends Serializable> boolean sendMessage(T msg) {
        if (msg == null)
            return false;

        if (protocol == null) {
            log.warning("Trying to send message to uninitialized drone: [{}]", ip);
            return false;
        } else {
            protocol.tell(msg, self());
            return true;
        }
    }

    private void setupDrone(final DroneDiscoveredMessage details, boolean reconnected) {
        // Assumes the drone is on the ground
        log.info("Discovery finished, forwarding connection details to protocol");
        protocol.tell(new DroneConnectionDetails(ip, details.getSendPort(), details.getRecvPort()), self());

        if(!reconnected) {
            sendMessage(new SetVideoStreamingStateCommand(false)); //disable video
            sendMessage(new SetOutdoorCommand(!indoor));
            sendMessage(new SetHullCommand(hull));
            sendMessage(new SetMaxHeightCommand(5)); //TODO: when rebooting commander, do not override these again
            sendMessage(new SetMaxTiltCommand(60f)); //default max tilt to 60 degrees
            sendMessage(new SetCountryCommand("BE")); //US code allows higher throughput regulations (breaks calibration?)
            sendMessage(new SetDateCommand(DateTime.now()));
            sendMessage(new SetTimeCommand(DateTime.now()));
            sendMessage(new FlatTrimCommand());
        }
        sendMessage(new RequestStatusCommand());
        sendMessage(new RequestSettingsCommand());
    }

    @Override
    protected void stop() {
        stopDiscovery();
        stopProtocol();
    }

    private void stopProtocol() {
        if (protocol != null) {
            protocol.tell(new StopMessage(), self());
            protocol = null;
        }
    }

    private void stopDiscovery() {
        if (discoveryProtocol != null) {
            discoveryProtocol.tell(new StopMessage(), self());
        }
        discoveryProtocol = null;
    }

    @Override
    protected void init(Promise<Void> p) {
        // Discovery protocol + setup actor
        synchronized (lock) {
            if (connectionStatus == ConnectionStatus.NOT_INITIALIZED) {
                initPromise = p;

                connectionStatus = ConnectionStatus.CONNECTING;

                //TODO: dispose each time when udp bound is fixed
                protocol = getContext().actorOf(Props.create(ArDrone3.class,
                        () -> new ArDrone3(d2cPort, Bebop.this.self())), "protocol"); // Initialize listening already before broadcasting itself

                discoveryProtocol = getContext().actorOf(Props.create(ArDrone3Discovery.class,
                        () -> new ArDrone3Discovery(ip, Bebop.this.self(), d2cPort)), "discovery");
            }
        }
    }

    @Override
    protected void setConnectionStatus(boolean connected) {
        super.setConnectionStatus(connected);
        connectionStatus = connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;

        if (!connected) {
            // Schedule reconnect
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(reconnectBackoff, TimeUnit.SECONDS),
                    self(), "reconnect", getContext().dispatcher(), null);
        }
    }

    private void handleReconnect() {
        // Check if it is still not online after the RECONNECT_BACKOFF timespan
        if (!isOnline.getRawValue()) {
            log.warning("Reconnection procedure for Bebop (backoff={})", reconnectBackoff);
            connectionStatus = ConnectionStatus.RECONNECTING;

            // Initiate new discovery
            discoveryProtocol = getContext().actorOf(Props.create(ArDrone3Discovery.class,
                    () -> new ArDrone3Discovery(ip, Bebop.this.self(), d2cPort)), "discovery");
        }
    }

    @Override
    protected void takeOff(Promise<Void> p) {
        //TODO: only return when status changes to taking off promises
        if (sendMessage(new TakeOffCommand())) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void land(Promise<Void> p) {
        if (sendMessage(new LandCommand())) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void move3d(Promise<Void> p, double vx, double vy, double vz, double vr) {
        if (sendMessage(new MoveCommand(vx, vy, vz, vr))) {
            p.success(null); //ack the command
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {
        if (sendMessage(new SetMaxHeightCommand(meters))) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {
        if (sendMessage(new SetMaxTiltCommand(degrees))) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {
        if (sendMessage(new SetOutdoorCommand(outdoor))) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {
        if (sendMessage(new SetHullCommand(hull))) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void flatTrim(Promise<Void> p) {
        if (sendMessage(new FlatTrimCommand())) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void reset(Promise<Void> p) {
        p.failure(new DroneException("Not implemented"));
    }

    @Override
    protected void flip(Promise<Void> p, FlipType type) {
        if (sendMessage(new FlipCommand(type))) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void initVideo(Promise<Void> p) {
       if(sendMessage(new InitVideoCommand())){
           p.success(null);
       } else {
           p.failure(new DroneException("Failed to send command. Not initialized yet."));
       }
    }

    @Override
    protected void stopVideo(Promise<Void> p) {
        if(sendMessage(new StopVideoCommand())){
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void emergency(Promise<Void> p) {
        land(p);
    }
}
