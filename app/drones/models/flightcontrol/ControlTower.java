package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control to control multiple drones
 */
public abstract class ControlTower extends FlightControl{
    public ControlTower(ActorRef actorRef) {
        super(actorRef);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        //TO DO
        return null;
    }

    @Override
    public void start() {

    }
}
