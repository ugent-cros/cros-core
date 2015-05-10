package drones.flightcontrol.messages;

import akka.actor.ActorRef;
import droneapi.model.properties.Location;

/**
 * Message as reply on a RequestGrantedMessage when the request for landing or take off, that is granted,
 * has been completed (the landing or take off is successfully executed). Sent from the pilot to the controlTower.
 *
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
