package drones.models.flightcontrol;

import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public class LandingCompletedMessage extends LocationMessage{

    public LandingCompletedMessage(Location location) {
        super(location);
    }

}
