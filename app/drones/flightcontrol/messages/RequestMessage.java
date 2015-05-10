package drones.flightcontrol.messages;

import akka.actor.ActorRef;
import droneapi.model.properties.Location;

/**
 * Message to request a landing or a take off. Sent from the pilot to the controlTower.
 *
 * Created by Sander on 26/03/2015.
 */
public class RequestMessage extends AbstractFlightControlMessage{

    private long droneId;

    public RequestMessage(ActorRef requester, Location location, AbstractFlightControlMessage.RequestType type, long droneId) {
        super(requester, location, type);
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
