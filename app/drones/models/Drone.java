package drones.models;

import akka.actor.AbstractActor;
import scala.concurrent.Future;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class Drone extends AbstractActor implements DroneControl, DroneStatus{
    protected LazyProperty<FlyingState> status;
    protected LazyProperty<Location> location;
    protected LazyProperty<Byte> batteryPercentage;
    protected LazyProperty<Boolean> flatTrimStatus;

    public Drone(){
        status = new LazyProperty<>(FlyingState.LANDED);
        location = new LazyProperty<>();
        batteryPercentage = new LazyProperty<>();
        flatTrimStatus = new LazyProperty<>(false);
    }

    public Future<Byte> getBatteryPercentage(){
        return batteryPercentage.getValue();
    }

    public Future<Location> getLocation() {
        return location.getValue();
    }

    public Future<FlyingState> getStatus() {
        return status.getValue();
    }
}
