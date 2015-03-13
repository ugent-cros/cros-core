package drones.models;

import akka.actor.ActorRef;
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

    public DroneCommander(final ActorRef droneActor) {
        this.droneActor = droneActor;
    }

    @Override
    public Future<Void> init() {
        return ask(droneActor, new InitRequestMessage(), INIT_TIMEOUT).map(new Mapper<Object, Void>() {
            public Void apply(Object s) {
                return null;
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Void> takeOff() {
        return ask(droneActor, new TakeOffRequestMessage(), TIMEOUT).map(new Mapper<Object, Void>() {
            public Void apply(Object s) {
                return null;
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Void> land() {
        return ask(droneActor, new LandRequestMessage(), TIMEOUT).map(new Mapper<Object, Void>() {
            public Void apply(Object s) {
                return null;
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<FlyingState> getFlyingState() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.FLYINGSTATE), TIMEOUT).map(new Mapper<Object, FlyingState>() {
            public FlyingState apply(Object s) {
                return (FlyingState) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Location> getLocation() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.LOCATION), TIMEOUT).map(new Mapper<Object, Location>() {
            public Location apply(Object s) {
                return (Location) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Byte> getBatteryPercentage() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.BATTERY), TIMEOUT).map(new Mapper<Object, Byte>() {
            public Byte apply(Object s) {
                return (Byte) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Double> getAltitude() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.ALTITUDE), TIMEOUT).map(new Mapper<Object, Double>() {
            public Double apply(Object s) {
                return (Double) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Rotation> getRotation() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.ROTATION), TIMEOUT).map(new Mapper<Object, Rotation>() {
            public Rotation apply(Object s) {
                return (Rotation) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Speed> getSpeed() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.SPEED), TIMEOUT).map(new Mapper<Object, Speed>() {
            public Speed apply(Object s) {
                return (Speed) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<DroneVersion> getVersion() {
        return ask(droneActor, new PropertyRequestMessage(PropertyType.VERSION), TIMEOUT).map(new Mapper<Object, DroneVersion>() {
            public DroneVersion apply(Object s) {
                return (DroneVersion) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }
}