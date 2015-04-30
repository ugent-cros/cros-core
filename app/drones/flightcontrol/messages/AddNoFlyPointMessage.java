package drones.flightcontrol.messages;

import droneapi.model.properties.Location;

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
