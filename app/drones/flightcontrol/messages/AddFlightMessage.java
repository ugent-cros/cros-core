package drones.flightcontrol.messages;

import models.Checkpoint;

import java.util.List;

/**
 * Created by Sander on 26/03/2015.
 */
public class AddFlightMessage extends AbstractIdFlightControlMessage{

    private List<Checkpoint> waypoints;

    public AddFlightMessage(long id, List<Checkpoint> waypoints) {
        super(id);
        this.waypoints = waypoints;
    }

    public List<Checkpoint> getWaypoints() {
        return waypoints;
    }
}
