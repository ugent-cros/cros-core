package drones.models.flightcontrol.messages;

import drones.models.flightcontrol.messages.AbstractIdFlightControlMessage;

/**
 * Created by Sander on 26/03/2015.
 */
public class RemoveFlightCompletedMessage extends AbstractIdFlightControlMessage {

    public RemoveFlightCompletedMessage(Long id) {
        super(id);
    }
}
