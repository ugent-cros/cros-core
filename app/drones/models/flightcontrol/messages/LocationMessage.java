package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

import java.io.Serializable;

/**
 * Created by Sander on 23/03/2015.
 */
public abstract class LocationMessage implements Serializable{

    private Location location;

    private ActorRef requester;

    private RequestType type;

    public LocationMessage(ActorRef requester, Location location, RequestType type) {
        this.requester = requester;
        this.location = location;
        this.type = type;
    }

    public LocationMessage(LocationMessage m){
        this.requester = m.getRequester();
        this.location = m.getLocation();
        this.type = m.getType();
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

}
