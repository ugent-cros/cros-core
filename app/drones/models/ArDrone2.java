package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.*;
import drones.messages.InitCompletedMessage;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by brecht on 3/9/15.
 */
public class ArDrone2 extends DroneActor {

    private ActorRef protocol;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Promise<Void> initPromise;
    private final boolean indoor;
    private final boolean hull;
    private final String ip;
    private final Object lock = new Object();


    public ArDrone2(String ip, boolean indoor, boolean hull) {
        this.ip = ip;
        this.indoor = indoor;
        this.hull = hull;
    }

    @Override
    protected void init(Promise<Void> p) {
        log.info("[ARDRONE2 MODEL] Starting ARDrone 2.0 INIT");

        // Setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = p;

                protocol = getContext().actorOf(Props.create(drones.protocols.ardrone2.ArDrone2.class,
                        () -> new drones.protocols.ardrone2.ArDrone2(new DroneConnectionDetails(ip, 5556, 5554), ArDrone2.this.self())));
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
        if (sendMessage(new EmergencyCommand())) {
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void takeOff(Promise<Void> p) {
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
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void moveToLocation(Promise<Void> p, double latitude, double longitude, double altitude) {
        p.failure(new DroneException("Command not implemented"));
    }

    @Override
    protected void cancelMoveToLocation(Promise<Void> p) {
        p.failure(new DroneException("Command not implemented"));
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
        if (sendMessage(new ResetCommand())) {
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
