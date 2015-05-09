package drones.flightcontrol.messages;

/**
 * Sent from a controlTower when the remove of a flight has been completed.
 *
 * Created by Sander on 26/03/2015.
 */
public class RemoveFlightCompletedMessage extends AbstractIdFlightControlMessage {

    public RemoveFlightCompletedMessage(long id) {
        super(id);
    }
}
