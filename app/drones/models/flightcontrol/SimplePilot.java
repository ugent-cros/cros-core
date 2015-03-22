package drones.models.flightcontrol;

import java.util.List;

import models.Checkpoint;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 */
public class SimplePilot extends Pilot{

	private List<Checkpoint> waypoints;

    public SimplePilot(Drone drone, List<Checkpoint> waypoints) {
        super(drone);
        this.waypoints = waypoints;
    }

    @Override
    public void start() {
        dc.init();
    }
}
