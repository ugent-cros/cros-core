package drones.models.flightcontrol.messages;

import drones.models.flightcontrol.messages.AbstractIdFlightControlMessage;

/**
 * Created by Sander on 26/03/2015.
 */
public class WayPointCompletedMessage extends AbstractIdFlightControlMessage {

    private int waypointNumber;

    public WayPointCompletedMessage(Long id, int waypointNumber) {
        super(id);
        this.waypointNumber = waypointNumber;
    }

    public int getWaypointNumber() {
        return waypointNumber;
    }
}
