package drones.models.flightcontrol.messages;

import drones.models.Location;

/**
 * Created by Sander on 9/04/2015.
 */
public class TakeOffCompletedMessage extends LocationMessage{

    public TakeOffCompletedMessage(Location location) {
        super(location);
    }

}
