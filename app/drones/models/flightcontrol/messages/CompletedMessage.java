package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class CompletedMessage extends LocationMessage{

    public CompletedMessage(LocationMessage.RequestType requestType,ActorRef requester, Location location) {
        super(requester, location, requestType);
    }

    public CompletedMessage(LocationMessage m) {
        super(m);
    }
}
