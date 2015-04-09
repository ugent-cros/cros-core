package drones.models.flightcontrol.messages;

import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestForLandingMessage extends LocationMessage{

    public RequestForLandingMessage(Location location) {
        super(location);
    }

}
