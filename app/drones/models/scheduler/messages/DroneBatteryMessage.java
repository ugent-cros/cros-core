package drones.models.scheduler.messages;


import model.properties.Location;

import java.io.Serializable;

/**
 * Created by Ronald on 23/03/2015.
 */
public class DroneBatteryMessage implements Serializable{

    private long droneId;
    private Location location;
    private int batteryPercentage;

    public DroneBatteryMessage(long droneId, Location location, int batteryPercentage) {
        this.droneId = droneId;
        this.location = location;
        this.batteryPercentage = batteryPercentage;
    }

    public long getDroneId() {
        return droneId;
    }

    public Location getLocation() {
        return location;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }
}
