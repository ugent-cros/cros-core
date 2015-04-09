package drones.models.flightcontrol.messages;

import drones.models.Location;

/**
 * Created by Sander on 9/04/2015.
 */
public class RequestForTakeOffMessage extends LocationMessage{

    public RequestForTakeOffMessage(Location location) {
        super(location);
    }
}