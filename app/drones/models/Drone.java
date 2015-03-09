package drones.models;

import akka.actor.ActorRef;
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

    private final ActorRef droneActor;

    public Drone(final ActorRef droneActor){
        this.droneActor = droneActor;
    }

    @Override
    public Future<Void> init() {
        return ask(droneActor, new InitRequestMessage(), TIMEOUT).map(s -> (Void) s, Akka.system().dispatcher());
    }

    @Override
    public Future<Void> takeOff() {
        return ask(droneActor, new TakeOffRequestMessage(), TIMEOUT).map(s -> (Void) s, Akka.system().dispatcher());
    }

    @Override
    public Future<Void> land() {
        return null;
    }

    @Override
    public Future<FlyingState> getFlyingState() {
        return ask(droneActor, new FlyingStateRequestMessage(), TIMEOUT).map(s -> (FlyingState) s, Akka.system().dispatcher());
    }

    @Override
    public Future<Location> getLocation() {
        return ask(droneActor, new LocationRequestMessage(), TIMEOUT).map(s -> (Location)s, Akka.system().dispatcher());
    }

    @Override
    public Future<Byte> getBatteryPercentage() {
        return ask(droneActor, new BatteryPercentageRequestMessage(), TIMEOUT).map(s -> (Byte)s, Akka.system().dispatcher());
    }
}
