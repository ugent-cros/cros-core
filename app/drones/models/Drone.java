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
public class Drone implements DroneControl, DroneStatus {

    private static final Timeout TIMEOUT = new Timeout(Duration.create(2, TimeUnit.SECONDS));
    private static final Timeout INIT_TIMEOUT = new Timeout(Duration.create(100, TimeUnit.SECONDS));

    private final ActorRef droneActor;

    public Drone(final ActorRef droneActor) {
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
        return ask(droneActor, new FlyingStateRequestMessage(), TIMEOUT).map(new Mapper<Object, FlyingState>() {
            public FlyingState apply(Object s) {
                return (FlyingState) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Location> getLocation() {
        return ask(droneActor, new LocationRequestMessage(), TIMEOUT).map(new Mapper<Object, Location>() {
            public Location apply(Object s) {
                return (Location) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }

    @Override
    public Future<Byte> getBatteryPercentage() {
        return ask(droneActor, new BatteryPercentageRequestMessage(), TIMEOUT).map(new Mapper<Object, Byte>() {
            public Byte apply(Object s) {
                return (Byte) ((ExecutionResultMessage) s).getValue();
            }
        }, Akka.system().dispatcher());
    }
}
