package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.*;
import drones.messages.DroneDiscoveredMessage;
import drones.messages.HomeChangedMessage;
import drones.messages.StopMessage;
import drones.protocols.ArDrone3;
import drones.protocols.ArDrone3Discovery;
import drones.util.LocationNavigator;
import org.joda.time.DateTime;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class Bepop extends NavigatedDroneActor {

    private ActorRef protocol;
    private ActorRef discoveryProtocol;

    private final String ip;
    private final boolean indoor;
    private final boolean hull;

    private final Object lock = new Object();

    private Promise<Void> initPromise;

    private int d2cPort;

    //TODO: use configuration class to pass here
    public Bepop(int d2cPort, String ip, boolean indoor, boolean hull) {
        this.hull = hull;
        this.d2cPort = d2cPort;
        this.ip = ip;
        this.indoor = indoor;
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(DroneDiscoveredMessage.class, this::handleDroneDiscoveryResponse);
    }

    @Override
    protected LocationNavigator createNavigator(Location currentLocation, Location goal) {
        return new LocationNavigator(currentLocation, goal,
                2f, 40f, 0.4f); // Bebop parameters
    }

    private void handleDroneDiscoveryResponse(DroneDiscoveredMessage s) {
        if (s.getStatus() == DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED) {
            //TODO: https://github.com/akka/akka/issues/15882 fix unbound when failed
            // protocol.tell(new StopMessage(), self()); // Stop the protocol (and bind)
            initPromise.failure(new DroneException("Failed to get drone discovery response."));
        } else {
            setupDrone(s);

            initPromise.success(null);
        }
        initPromise = null;
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

    private void setupDrone(final DroneDiscoveredMessage details) {
        // Assumes the drone is on the ground
        log.info("Discovery finished, forwarding connection details to protocol");
        protocol.tell(new DroneConnectionDetails(ip, details.getSendPort(), details.getRecvPort()), self());

        sendMessage(new SetVideoStreamingStateCommand(false)); //disable video
        sendMessage(new SetOutdoorCommand(!indoor));
        sendMessage(new SetHullCommand(hull));
        sendMessage(new SetMaxHeightCommand(5)); //TODO: when rebooting commander, do not override these again
        sendMessage(new SetMaxTiltCommand(60f)); //default max tilt to 60 degrees
        sendMessage(new SetCountryCommand("BE")); //US code allows higher throughput regulations (breaks calibration?)
        sendMessage(new SetDateCommand(DateTime.now()));
        sendMessage(new SetTimeCommand(DateTime.now()));
        sendMessage(new RequestStatusCommand());
        sendMessage(new RequestSettingsCommand());
        sendMessage(new FlatTrimCommand());
    }

    @Override
    protected void init(Promise<Void> p) {
        // Discovery protocol + setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = p;

                if (protocol == null) {
                    //TODO: dispose each time when udp bound is fixed
                    protocol = getContext().actorOf(Props.create(ArDrone3.class,
                            () -> new ArDrone3(d2cPort, Bepop.this.self()))); // Initialize listening already before broadcasting itself
                }


                if (discoveryProtocol != null) {
                    discoveryProtocol.tell(new StopMessage(), self());
                }
                discoveryProtocol = getContext().actorOf(Props.create(ArDrone3Discovery.class,
                        () -> new ArDrone3Discovery(ip, Bepop.this.self(), d2cPort)));
            }
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
        if(sendMessage(new MoveCommand(vx, vy, vz, vr))){
            p.success(null); //ack the command
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {
        if(sendMessage(new SetMaxHeightCommand(meters))){
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {
        if(sendMessage(new SetMaxTiltCommand(degrees))){
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {
        if(sendMessage(new SetOutdoorCommand(outdoor))){
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {
        if(sendMessage(new SetHullCommand(hull))){
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void flatTrim(Promise<Void> p) {
        if(sendMessage(new FlatTrimCommand())){
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
        if(sendMessage(new FlipCommand(type))){
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
