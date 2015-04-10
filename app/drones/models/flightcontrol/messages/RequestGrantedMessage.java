package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestGrantedMessage extends LocationMessage{

    public RequestGrantedMessage(RequestType requestType, ActorRef requestor, Location location) {
        super(requestor,location, requestType);
    }

    public RequestGrantedMessage(LocationMessage m) {
        super(m);
    }
}
