package parrot.ardrone2;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import droneapi.model.NavigatedDroneActor;
import parrot.ardrone2.protocol.ArDrone2Protocol;
import parrot.messages.InitCompletedMessage;
import parrot.shared.commands.*;
import parrot.shared.models.DroneConnectionDetails;
import droneapi.messages.StopMessage;
import droneapi.model.DroneException;
import droneapi.model.properties.FlipType;
import droneapi.model.properties.Location;
import droneapi.navigator.LocationNavigator;
import parrot.shared.commands.MoveCommand;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by brecht on 3/9/15.
 */
public class ArDrone2 extends NavigatedDroneActor {

    private ActorRef protocol;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Promise<Void> initPromise;
    private final boolean indoor;
    private final boolean hull;
    private final String ip;
    private final Object lock = new Object();
    private static final String NOT_IMPLEMENTED = "Command not implemented";

    public ArDrone2(String ip, boolean indoor, boolean hull) {
        this.ip = ip;
        this.indoor = indoor;
        this.hull = hull;
    }

    @Override
    protected LocationNavigator createNavigator(Location currentLocation, Location goal) {
        return new LocationNavigator(currentLocation, goal,
                2f, 30f, 1f);
    }

    @Override
    protected void stop() {
        if (protocol != null) {
            protocol.tell(new StopMessage(), self());
            protocol = null;
        }
    }

    @Override
    protected void init(Promise<Void> p) {
        log.info("[ARDRONE2 MODEL] Starting ARDrone 2.0 INIT");

        // Setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = p;

                protocol = getContext().actorOf(Props.create(ArDrone2Protocol.class,
                        () -> new ArDrone2Protocol(new DroneConnectionDetails(ip, 5556, 5554), ArDrone2.this.self())));
            }
        }

        p.success(null);
    }

    private void handleInitCompletedResponse() {
        log.info("[ARDRONE2 MODEL] ArDrone Ping message received");
        setupDrone();
    }

    private void setupDrone() {
        log.info("[ARDRONE2 MODEL] Forwarding connection details to protocol");
        protocol.tell(new DroneConnectionDetails(ip, 5556, 5556), self());

        sendMessage(new InitDroneCommand());
        sendMessage(new SetOutdoorCommand(!indoor));
        sendMessage(new SetHullCommand(hull));
    }

    @Override
    protected void emergency(Promise<Void> p) {
        sendCommand(p, new EmergencyCommand());
    }

    @Override
    protected void takeOff(Promise<Void> p) {
        sendCommand(p, new TakeOffCommand());
    }

    @Override
    protected void land(Promise<Void> p) {
        sendCommand(p, new LandCommand());
    }

    @Override
    protected void move3d(Promise<Void> p, double vx, double vy, double vz, double vr) {
        sendCommand(p, new MoveCommand(vx, vy, vz, vr));
    }

    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {
        sendCommand(p, new SetMaxHeightCommand(meters));
    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {
        p.failure(new DroneException(NOT_IMPLEMENTED));
    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {
        sendCommand(p, new SetOutdoorCommand(outdoor));
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {
        sendCommand(p, new SetHullCommand(hull));
    }

    @Override
    protected void flatTrim(Promise<Void> p) {
        sendCommand(p, new FlatTrimCommand());
    }

    @Override
    protected void reset(Promise<Void> p) {
        sendCommand(p, new ResetCommand());
    }

    @Override
    protected void flip(Promise<Void> p, FlipType type) {
        sendCommand(p, new FlipCommand(type));
    }

    @Override
    protected void initVideo(Promise<Void> p) {
        sendCommand(p, new InitVideoCommand());
    }

    @Override
    protected void stopVideo(Promise<Void> p) {
        sendCommand(p, new StopVideoCommand());
    }

    private void sendCommand(Promise<Void> p, Serializable command) {
        if (sendMessage(command)) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(InitCompletedMessage.class, s -> handleInitCompletedResponse());
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
}
