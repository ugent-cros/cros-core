package drones.models.scheduler;

import drones.models.Location;
import models.Drone;

import java.io.Serializable;

/**
 * Created by Ronald on 23/03/2015.
 */
public class DroneBatteryMessage implements Serializable{

    private Drone drone;
    private Location location;
    private int batteryPercentage;

    public DroneBatteryMessage(Drone drone, Location location, int batteryPercentage) {
        this.drone = drone;
        this.location = location;
        this.batteryPercentage = batteryPercentage;
    }

    public Drone getDrone() {
        return drone;
    }

    public Location getLocation() {
        return location;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }
}
