package drones.models.flightcontrol;

import java.util.List;

import akka.dispatch.OnSuccess;
import models.Checkpoint;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 * 
 * Pilot class to fly with the drone to its destination via the waypoints.
 * He lands on the last item in the list.
 */
public class SimplePilot extends Pilot{

	private List<Checkpoint> waypoints;

    public SimplePilot(Drone drone, List<Checkpoint> waypoints) {
        super(drone);

        if(waypoints.size() < 1){
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.waypoints = waypoints;
    }

    @Override
    public void start() {
        if (altitude == 0) {
            altitude = DEFAULT_ALTITUDE;
        }

    }
}
