package drones.models;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.util.Timeout;
import drones.messages.*;
import play.libs.Akka;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * Created by Cedric on 3/9/2015.
 */
public class DroneCommander implements DroneControl, DroneStatus {

    private static final Timeout TIMEOUT = new Timeout(Duration.create(2, TimeUnit.SECONDS));

    private static final Timeout INIT_TIMEOUT = new Timeout(Duration.create(100, TimeUnit.SECONDS));

    private final ActorRef droneActor;

    private boolean initialized = false;
    private boolean shutdown = false;

    public DroneCommander(final ActorRef droneActor) {
        this.droneActor = droneActor;
    }

    public boolean canSend(){
        return initialized && !shutdown;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private <T> Future<T> noDroneConnection(){
        if(initialized && shutdown){
            return Futures.failed(new DroneException("DroneCommander was shut down previously."));
        } else {
            return Futures.failed(new DroneException("DroneCommander not initialized yet."));
        }
    }

    @Override
    public Future<Void> init() {
        if (!canSend()) {
            return ask(droneActor, new InitRequestMessage(), INIT_TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    initialized = true;
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return Futures.failed(new DroneException("Drone already initialized/stopped."));
    }

    @Override
    public Future<Void> takeOff() {
        if(canSend()) {
            return ask(droneActor, new TakeOffRequestMessage(), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> land() {
        if(canSend()) {
            return ask(droneActor, new LandRequestMessage(), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> move3d(double vx, double vy, double vz, double vr) {
        if(canSend()) {
            return ask(droneActor, new MoveRequestMessage(vx, vy, vz, vr), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> move(double vx, double vy, double vr) {
        return move3d(vx, vy, 0d, vr);
    }

    @Override
    public Future<Void> setMaxHeight(float meters) {
        if(meters <= 0.5)
            return Futures.failed(new IllegalArgumentException("Max height cannot be lower than 0.5m"));

        if(canSend()) {
            return ask(droneActor, new SetMaxHeightRequestMessage(meters), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> setMaxTilt(float degrees) {
        if(degrees < 0)
            return Futures.failed(new IllegalArgumentException("Max tilt cannot be negative."));

        if(canSend()) {
            return ask(droneActor, new SetMaxTiltRequestMessage(degrees), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> moveToLocation(double latitude, double longitude, double altitude) {
        // Lat is bound by 90 degrees (north/south), longitude by 180 east/west
        if(Math.abs(latitude) > 90.0d || Math.abs(longitude) > 180.0d || altitude <= 0.0d)
            return Futures.failed(new IllegalArgumentException("invalid coordinates"));

        if(canSend()) {
            return ask(droneActor, new MoveToLocationRequestMessage(latitude, longitude, altitude), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> cancelMoveToLocation() {
        if(canSend()) {
            return ask(droneActor, new MoveToLocationCancellationMessage(), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> calibrate(boolean outdoor, boolean hull) {
        if(canSend()) {
            return ask(droneActor, new CalibrateRequestMessage(hull, outdoor), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> flatTrim() {
        if(canSend()) {
            return ask(droneActor, new FlatTrimRequestMessage(), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> setOutdoor(boolean outdoor) {
        if(canSend()) {
            return ask(droneActor, new SetOutdoorRequestMessage(outdoor), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> setHull(boolean hull) {
        if(canSend()) {
            return ask(droneActor, new SetHullRequestMessage(hull), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Void> flip(FlipType type) {
        if(canSend()) {
            return ask(droneActor, new FlipRequestMessage(type), TIMEOUT).map(new Mapper<Object, Void>() {
                public Void apply(Object s) {
                    return null;
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public void stop() {
        droneActor.tell(new StopMessage(), ActorRef.noSender());
        shutdown = true;
    }

    @Override
    public Future<FlyingState> getFlyingState() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.FLYINGSTATE), TIMEOUT).map(new Mapper<Object, FlyingState>() {
                public FlyingState apply(Object s) {
                    return (FlyingState) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Location> getLocation() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.LOCATION), TIMEOUT).map(new Mapper<Object, Location>() {
                public Location apply(Object s) {
                    return (Location) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Byte> getBatteryPercentage() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.BATTERY), TIMEOUT).map(new Mapper<Object, Byte>() {
                public Byte apply(Object s) {
                    return (Byte) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Double> getAltitude() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.ALTITUDE), TIMEOUT).map(new Mapper<Object, Double>() {
                public Double apply(Object s) {
                    return (Double) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Rotation> getRotation() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.ROTATION), TIMEOUT).map(new Mapper<Object, Rotation>() {
                public Rotation apply(Object s) {
                    return (Rotation) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Speed> getSpeed() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.SPEED), TIMEOUT).map(new Mapper<Object, Speed>() {
                public Speed apply(Object s) {
                    return (Speed) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<DroneVersion> getVersion() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.VERSION), TIMEOUT).map(new Mapper<Object, DroneVersion>() {
                public DroneVersion apply(Object s) {
                    return (DroneVersion) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<NavigationState> getNavigationState() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.NAVIGATIONSTATE), TIMEOUT).map(new Mapper<Object, NavigationState>() {
                public NavigationState apply(Object s) {
                    return (NavigationState) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<NavigationStateReason> getNavigationStateReason() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.NAVIGATIONREASON), TIMEOUT).map(new Mapper<Object, NavigationStateReason>() {
                public NavigationStateReason apply(Object s) {
                    return (NavigationStateReason) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Boolean> isGPSFixed() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.GPSFIX), TIMEOUT).map(new Mapper<Object, Boolean>() {
                public Boolean apply(Object s) {
                    return (Boolean) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Boolean> isOnline() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.NETWORK_STATUS), TIMEOUT).map(new Mapper<Object, Boolean>() {
                public Boolean apply(Object s) {
                    return (Boolean) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    @Override
    public Future<Boolean> isCalibrationRequired() {
        if(canSend()) {
            return ask(droneActor, new PropertyRequestMessage(PropertyType.CALIBRATION_REQUIRED), TIMEOUT).map(new Mapper<Object, Boolean>() {
                public Boolean apply(Object s) {
                    return (Boolean) ((ExecutionResultMessage) s).getValue();
                }
            }, Akka.system().dispatcher());
        } else return noDroneConnection();
    }

    /**
     * Subscribe to messages of given topic
     *
     * @param sub The actor to which the events have to be sent
     * @param cl  The topic class of the message to subscribe to
     */
    public void subscribeTopic(final ActorRef sub, Class cl) {
        subscribeTopics(sub, new Class[] { cl });
    }

    /**
     * Subscribe to message of given topics
     * @param sub The actor to which the events have to be sent
     * @param topics The topic class of the message to subscribe to
     */
    public void subscribeTopics(final ActorRef sub, Class[] topics){
        if(canSend()) {
            droneActor.tell(new SubscribeEventMessage(topics), sub);
        } else throw new DroneException("Drone already shutdown.");
    }

    /**
     * Unsubscribe for a given topic
     *
     * @param sub The currently subscribed actor
     * @param cl  The topic class of the messages to unsubscribe
     */
    public void unsubscribeTopic(final ActorRef sub, Class cl) {
        if(canSend()) {
            droneActor.tell(new UnsubscribeEventMessage(cl), sub);
        } else throw new DroneException("Drone Already shutdown.");
    }

    /**
     * Unsubscribe all messages
     *
     * @param sub The actor to unsubscribe
     */
    public void unsubscribe(final ActorRef sub) {
        if(canSend()) {
            droneActor.tell(new UnsubscribeEventMessage(), sub);
        } else throw new DroneException("Drone Already shutdown.");
    }
}
