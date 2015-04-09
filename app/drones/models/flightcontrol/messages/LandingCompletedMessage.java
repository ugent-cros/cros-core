package drones.models.flightcontrol.messages;

import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class LandingCompletedMessage extends LocationMessage{

    public LandingCompletedMessage(Location location) {
        super(location);
    }

}
