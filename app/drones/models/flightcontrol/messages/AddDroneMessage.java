package drones.models.flightcontrol.messages;

import models.Checkpoint;
import models.Drone;

import java.io.Serializable;
import java.util.List;

/**
 *
 * Created by Sander on 10/04/2015.
 */
public class AddDroneMessage implements Serializable {

    private Long droneId;
    private List<Checkpoint> waypoints;

    public AddDroneMessage(Long droneId, List<Checkpoint> waypoints) {
        this.waypoints = waypoints;
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }

    public List<Checkpoint> getWaypoints() {
        return waypoints;
    }
}
