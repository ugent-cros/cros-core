package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class LandingCompletedMessage extends LocationMessage{

    public LandingCompletedMessage(ActorRef requestor, Location location) {
        super(requestor, location, RequestType.LANDING);
    }

}
