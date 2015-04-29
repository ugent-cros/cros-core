package drones.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

/**
 * Created by Sander on 9/04/2015.
 */
public class TakeOffCompletedMessage extends LocationMessage{

    public TakeOffCompletedMessage(ActorRef requestor, Location location) {
        super(requestor,location, RequestType.TAKEOFF);
    }

}
