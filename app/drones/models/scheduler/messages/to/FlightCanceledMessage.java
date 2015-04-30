package drones.models.scheduler.messages.to;

import drones.models.flightcontrol.messages.AbstractIdFlightControlMessage;

/**
 * Created by Ronald on 17/04/2015.
 */
public class FlightCanceledMessage extends AbstractIdFlightControlMessage{

    public FlightCanceledMessage(Long id) {
        super(id);
    }
}
