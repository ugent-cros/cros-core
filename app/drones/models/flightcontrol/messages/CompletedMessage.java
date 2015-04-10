package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class CompletedMessage extends LocationMessage{

    public CompletedMessage(RequestType requestType,ActorRef requestor, Location location) {
        super(requestor, location, requestType);
    }

    public CompletedMessage(LocationMessage m) {
        super(m);
    }
}
