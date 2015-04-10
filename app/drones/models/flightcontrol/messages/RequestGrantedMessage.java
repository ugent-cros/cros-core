package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestGrantedMessage extends LocationMessage{

    public RequestGrantedMessage(RequestType requestType, ActorRef requester, Location location) {
        super(requester,location, requestType);
    }

    public RequestGrantedMessage(LocationMessage m) {
        super(m);
    }
}
