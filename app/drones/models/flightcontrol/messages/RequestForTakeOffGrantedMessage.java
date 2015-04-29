package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

/**
 * Created by Sander on 9/04/2015.
 */
public class RequestForTakeOffGrantedMessage extends LocationMessage{

    public RequestForTakeOffGrantedMessage(ActorRef requestor, Location location) {
        super(requestor,location, RequestType.TAKEOFF);
    }
}
