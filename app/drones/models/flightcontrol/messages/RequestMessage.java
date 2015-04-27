package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 26/03/2015.
 */
public class RequestMessage extends AbstractFlightControlMessage{

    private Long droneId;

    public RequestMessage(ActorRef requester, Location location, AbstractFlightControlMessage.RequestType type, Long droneId) {
        super(requester, location, type);
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}
