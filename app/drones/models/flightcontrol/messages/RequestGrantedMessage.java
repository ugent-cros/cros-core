package drones.models.flightcontrol.messages;

import akka.actor.ActorRef;
import drones.models.Location;

/**
 * Created by Sander on 23/03/2015.
 */
public class RequestGrantedMessage{

    RequestMessage m;

    public RequestGrantedMessage(RequestMessage m) {
        this.m = m;
    }

    public RequestMessage getRequestMessage() {
        return m;
    }
}
