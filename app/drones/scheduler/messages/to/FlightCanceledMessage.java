package drones.scheduler.messages.to;

import drones.flightcontrol.messages.AbstractIdFlightControlMessage;

/**
 * Sent to a flightControl to cancel the flight.
 *
 * Created by Ronald on 17/04/2015.
 */
public class FlightCanceledMessage extends AbstractIdFlightControlMessage{

    private boolean done;

    /**
     *
     * @param id droneId
     * @param done if the pilot is done
     */
    public FlightCanceledMessage(long id, boolean done) {
        super(id);
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }
}
