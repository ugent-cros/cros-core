package drones.models.flightcontrol.messages;

import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public abstract class LocationMessage implements Serializable{

    private Location location;

    public LocationMessage(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
