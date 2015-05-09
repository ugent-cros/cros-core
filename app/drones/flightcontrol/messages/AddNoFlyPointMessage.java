package drones.flightcontrol.messages;

import droneapi.model.properties.Location;

import java.io.Serializable;

/**
 * Sent to a pilot to add a noFlyPoint (= a point where the pilot may not fly).
 *
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
