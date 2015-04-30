package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

/**
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
