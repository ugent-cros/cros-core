package drones.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.flightcontrol.messages.AddFlightMessage;
import drones.flightcontrol.messages.FlightControlExceptionMessage;
import drones.flightcontrol.messages.RemoveFlightMessage;
import drones.flightcontrol.messages.WayPointCompletedMessage;
import drones.scheduler.messages.to.FlightCanceledMessage;
import drones.scheduler.messages.to.FlightCompletedMessage;

/**
 * A control tower ensures that multiple drones can fly without colliding.
 *
 * Created by Sander on 18/03/2015.
 */
public abstract class ControlTower extends FlightControl{

    /**
     *
     * @param reporterRef actor to report the outgoing messages
     */
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

    /**
     * Add a flight to the controlTower.
     */
    protected abstract void addFlightMessage(AddFlightMessage m);

    /**
     * Remove a flight from the controlTower.
     */
    protected abstract void removeFlightMessage(RemoveFlightMessage m);

    /**
     * Handles a FlightControlExceptionMessage sent by a pilot.
     */
    protected abstract void flightControlExceptionMessage(FlightControlExceptionMessage m);

    /**
     * Handles a FlightCompletedMessage sent by a pilot.
     */
    protected abstract void flightCompletedMessage(FlightCompletedMessage m);

    /**
     * Handles a FlightCanceledMessage sent by a pilot.
     */
    protected abstract void flightCanceledMessage(FlightCanceledMessage m);

    /**
     * Handles a WayPointCompletedMessage sent by a pilot.
     */
    protected abstract void wayPointCompletedMessage(WayPointCompletedMessage m);
}
