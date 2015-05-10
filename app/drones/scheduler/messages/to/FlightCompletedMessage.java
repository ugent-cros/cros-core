package drones.scheduler.messages.to;

import droneapi.model.properties.Location;

import java.io.Serializable;

/**
 * Sent from a pilot when the flight has been completed and the drone has landed.
 *
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
