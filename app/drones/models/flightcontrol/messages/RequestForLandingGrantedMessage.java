package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestForLandingGrantedMessage extends LocationMessage{

    public RequestForLandingGrantedMessage(ActorRef requestor, Location location) {
        super(requestor,location, RequestType.LANDING);
    }

}
