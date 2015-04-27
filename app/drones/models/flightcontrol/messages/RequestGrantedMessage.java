package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 26/03/2015.
 */
public class RequestGrantedMessage implements Serializable{

    private RequestMessage requestMessage;

    public RequestGrantedMessage(RequestMessage requestMessage) {
        this.requestMessage = requestMessage;
    }

    public RequestMessage getRequestMessage() {
        return requestMessage;
    }
}
