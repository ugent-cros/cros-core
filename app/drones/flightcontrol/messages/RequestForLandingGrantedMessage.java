package drones.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestForLandingGrantedMessage extends LocationMessage{

    public RequestForLandingGrantedMessage(ActorRef requestor, Location location) {
        super(requestor,location, RequestType.LANDING);
    }

}
