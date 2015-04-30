package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public abstract class AbstractFlightControlMessage implements Serializable{

    private Location location;

    private ActorRef requester;

    private RequestType type;

    public AbstractFlightControlMessage(ActorRef requester, Location location, RequestType type) {
        this.requester = requester;
        this.location = location;
        this.type = type;
    }

    public Location getLocation() {
        return location;
    }

    public ActorRef getRequester() {
        return requester;
    }

    public RequestType getType() {
        return type;
    }

    public enum RequestType {
        TAKEOFF,
        LANDING
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if(o instanceof AbstractFlightControlMessage){
            AbstractFlightControlMessage m = (AbstractFlightControlMessage) o;
            result = m.getLocation().equals(location) && m.getType().equals(type) && m.getRequester().equals(requester);
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + requester.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
