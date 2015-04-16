package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.DroneArrivalMessage;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control to control multiple drones
 */
public abstract class ControlTower extends FlightControl{

    public ControlTower(ActorRef reporterRef) {
        super(reporterRef);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(DroneArrivalMessage.class, s -> droneArrivalMessage(s)).
                match(AddDroneMessage.class, s -> addDroneMessage(s)).
                match(RemoveDroneMessage.class, s -> removeDroneMessage(s));
    }

    protected abstract void droneArrivalMessage(DroneArrivalMessage m);

    protected abstract void addDroneMessage(AddDroneMessage m);

    protected abstract void removeDroneMessage(RemoveDroneMessage m);
}
