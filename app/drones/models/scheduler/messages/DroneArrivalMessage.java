package drones.models.scheduler.messages;

import models.Drone;
import drones.models.DroneCommander;
import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Ronald on 18/03/2015.
 */
public class DroneArrivalMessage implements Serializable{

    private long droneId;
    private Location location;

    public DroneArrivalMessage(long droneId, Location location) {
        this.droneId = droneId;
        this.location = location;
    }

    public long getDroneId() {
        return droneId;
    }

    public Location getLocation() {
        return location;
    }
}
