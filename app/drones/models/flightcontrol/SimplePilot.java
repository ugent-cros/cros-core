package drones.models.flightcontrol;

import drones.models.DroneCommander;
import drones.models.Location;
import models.Drone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sander on 18/03/2015.
 */
public class SimplePilot extends Pilot{

    private List<Location> waypoints;

    public SimplePilot(Drone drone, List<Location> waypoints) {
        super(drone);
        this.waypoints = waypoints;
    }

    @Override
    public void start() {
        dc.init();
    }
}
