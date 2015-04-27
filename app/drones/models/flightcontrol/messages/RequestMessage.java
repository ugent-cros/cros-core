package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 26/03/2015.
 */
public class RequestMessage extends AbstractFlightControlMessage{

    public RequestMessage(ActorRef requester, Location location, RequestType type) {
        super(requester, location, type);
    }
}
