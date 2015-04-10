package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestMessage extends LocationMessage{

    public RequestMessage(RequestType requestType, ActorRef requestor, Location location) {
        super(requestor,location, requestType);
    }

    public RequestMessage(LocationMessage m) {
        super(m);
    }
}
