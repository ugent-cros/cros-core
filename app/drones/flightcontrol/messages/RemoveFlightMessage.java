package drones.flightcontrol.messages;

/**
 * Sent to a controlTower in order to remove a flight from it.
 *
 * Created by Sander on 26/03/2015.
 */
public class RemoveFlightMessage extends AbstractIdFlightControlMessage{

    public RemoveFlightMessage(long id) {
        super(id);
    }
}
