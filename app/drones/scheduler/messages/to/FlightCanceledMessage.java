package drones.scheduler.messages.to;

import drones.flightcontrol.messages.AbstractIdFlightControlMessage;

/**
 * Sent to a flightControl to cancel the flight.
 *
 * Created by Ronald on 17/04/2015.
 */
public class FlightCanceledMessage extends AbstractIdFlightControlMessage{

    public FlightCanceledMessage(Long id) {
        super(id);
    }
}
