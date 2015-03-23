package drones.models.flightcontrol;

import akka.actor.ActorRef;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control to control multiple drones
 */
public abstract class ControlTower extends FlightControl{
    public ControlTower(ActorRef actorRef) {
        super(actorRef);
    }
}
