package drones.flightcontrol.messages;

/**
 * Sent when a pilot arrives at a wayPoint (and the waiting time is completed).
 *
 * Created by Sander on 26/03/2015.
 */
public class WayPointCompletedMessage extends AbstractIdFlightControlMessage {

    private int waypointNumber;

    public WayPointCompletedMessage(long id, int waypointNumber) {
        super(id);
        this.waypointNumber = waypointNumber;
    }

    public int getWaypointNumber() {
        return waypointNumber;
    }
}
