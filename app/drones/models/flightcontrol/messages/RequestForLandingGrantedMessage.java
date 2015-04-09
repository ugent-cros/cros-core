package drones.models.flightcontrol.messages;

import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestForLandingGrantedMessage extends LocationMessage{

    public RequestForLandingGrantedMessage(Location location) {
        super(location);
    }

}
