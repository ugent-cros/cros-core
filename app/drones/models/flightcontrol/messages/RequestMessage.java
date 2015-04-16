package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Message to request permission to land or to take off.
 *
 * Created by Sander on 23/03/2015.
 */
public class RequestMessage extends LocationMessage{

    public RequestMessage(LocationMessage.RequestType requestType, ActorRef requester, Location location) {
        super(requester,location, requestType);
    }

    public RequestMessage(LocationMessage m) {
        super(m);
    }
}
