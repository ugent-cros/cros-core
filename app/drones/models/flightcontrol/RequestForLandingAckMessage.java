package drones.models.flightcontrol;

import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestForLandingAckMessage extends LocationMessage{

    public RequestForLandingAckMessage(Location location) {
        super(location);
    }

}
