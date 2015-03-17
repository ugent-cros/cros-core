package drones.models.scheduler;


import drones.models.Location;

import java.util.List;

/**
 * Created by Ronald on 16/03/2015.
 */
public class Assignment {

    private int priority;
    private List<Location> waypoints;

    public Assignment(List<Location> waypoints){
        this(waypoints,0);
    }

    public Assignment(List<Location> waypoints, int priority){
        this.waypoints = waypoints;
        this.priority = priority;
    }
}
