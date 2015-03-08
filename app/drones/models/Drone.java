package drones.models;

import akka.actor.AbstractActor;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class Drone extends AbstractActor implements DroneControl, DroneStatus{
    protected FlyingState status;

    public Drone(){
        status = FlyingState.LANDED;
    }

    public FlyingState getStatus() {
        return status;
    }
}
