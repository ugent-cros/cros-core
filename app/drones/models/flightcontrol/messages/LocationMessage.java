package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import model.properties.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public abstract class LocationMessage implements Serializable{

    private Location location;

    private ActorRef requestor;

    private RequestType type;

    public LocationMessage(ActorRef requestor, Location location, RequestType type) {
        this.requestor = requestor;
        this.location = location;
        this.type = type;
    }

    public Location getLocation() {
        return location;
    }

    public ActorRef getRequestor() {
        return requestor;
    }

    public RequestType getType() {
        return type;
    }

    public enum RequestType {
        TAKEOFF,
        LANDING
    }

}
