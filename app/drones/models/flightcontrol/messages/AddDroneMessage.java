package drones.models.flightcontrol.messages;

import models.Checkpoint;
import models.Drone;

import java.util.List;

/**
 * Created by Sander on 10/04/2015.
 */
public class AddDroneMessage {

    private Drone drone;
    private List<Checkpoint> waypoints;

    public AddDroneMessage(Drone drone, List<Checkpoint> waypoints) {
        this.waypoints = waypoints;
        this.drone = drone;
    }

    public Drone getDrone() {
        return drone;
    }

    public List<Checkpoint> getWaypoints() {
        return waypoints;
    }
}
