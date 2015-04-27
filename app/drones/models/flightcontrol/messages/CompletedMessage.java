package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 26/03/2015.
 */
public class CompletedMessage extends AbstractFlightControlMessage{

    public CompletedMessage(ActorRef requester, Location location, AbstractFlightControlMessage.RequestType type) {
        super(requester, location, type);
    }

    public CompletedMessage(AbstractFlightControlMessage m){
        super(m.getRequester(), m.getLocation(), m.getType());
    }
}
