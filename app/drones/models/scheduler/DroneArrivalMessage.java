package drones.models.scheduler;

import models.Drone;
import drones.models.DroneCommander;
import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Ronald on 18/03/2015.
 */
public class DroneArrivalMessage implements Serializable{

    private Drone drone;
    private Location location;

    public DroneArrivalMessage(Drone drone, Location location) {
        this.drone = drone;
        this.location = location;
    }

    public Drone getDrone() {
        return drone;
    }

    public Location getLocation() {
        return location;
    }
}
