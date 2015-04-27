package drones.models.flightcontrol.messages;

import models.Checkpoint;

import java.util.List;

/**
 * Created by Sander on 26/03/2015.
 */
public class AddDroneMessage extends AbstractIdFlightControlMessage{

    private List<Checkpoint> waypoints;

    public AddDroneMessage(Long id, List<Checkpoint> waypoints) {
        super(id);
        this.waypoints = waypoints;
    }

    public List<Checkpoint> getWaypoints() {
        return waypoints;
    }
}
