package drones.models.scheduler.messages.to;

import model.properties.Location;

import java.io.Serializable;

/**
 * Created by Ronald on 18/03/2015.
 */
public class FlightCompletedMessage implements Serializable{

    private long droneId;
    private Location location;

    public FlightCompletedMessage(long droneId, Location location) {
        this.droneId = droneId;
        this.location = location;
    }

    public long getDroneId() {
        return droneId;
    }

    // TODO: remove location
    public Location getLocation() {
        return location;
    }
}
