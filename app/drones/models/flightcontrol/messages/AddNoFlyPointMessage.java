package drones.models.flightcontrol.messages;

import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Sander on 10/04/2015.
 */
public class AddNoFlyPointMessage implements Serializable {

    Location noFlyPoint;

    public AddNoFlyPointMessage(Location noFlyPoint) {
        this.noFlyPoint = noFlyPoint;
    }

    public Location getNoFlyPoint() {
        return noFlyPoint;
    }
}
