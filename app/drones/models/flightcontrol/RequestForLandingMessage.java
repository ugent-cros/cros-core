package drones.models.flightcontrol;

import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestForLandingMessage extends LocationMessage{

    public RequestForLandingMessage(Location location) {
        super(location);
    }

}
