package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.messages.to.FlightCanceledMessage;
import drones.models.scheduler.messages.to.FlightCompletedMessage;

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
                match(AddFlightMessage.class, s -> addFlightMessage(s)).
                match(RemoveFlightMessage.class, s -> removeFlightMessage(s)).
                match(FlightControlExceptionMessage.class, s -> flightControlExceptionMessage(s)).
                match(FlightCompletedMessage.class, s -> flightCompletedMessage(s)).
                match(FlightCanceledMessage.class, s -> flightCanceledMessage(s)).
                match(WayPointCompletedMessage.class, s -> wayPointCompletedMessage(s));
    }

    protected abstract void addFlightMessage(AddFlightMessage m);

    protected abstract void removeFlightMessage(RemoveFlightMessage m);

    protected abstract void flightControlExceptionMessage(FlightControlExceptionMessage m);

    protected abstract void flightCompletedMessage(FlightCompletedMessage m);

    protected abstract void flightCanceledMessage(FlightCanceledMessage m);

    protected abstract void wayPointCompletedMessage(WayPointCompletedMessage m);
}