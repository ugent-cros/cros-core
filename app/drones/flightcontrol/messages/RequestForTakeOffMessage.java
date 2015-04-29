package drones.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

/**
 * Created by Sander on 9/04/2015.
 */
public class RequestForTakeOffMessage extends LocationMessage{

    public RequestForTakeOffMessage(ActorRef requestor, Location location) {
        super(requestor,location, RequestType.TAKEOFF);
    }
}
