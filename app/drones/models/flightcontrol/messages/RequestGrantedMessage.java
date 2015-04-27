package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 26/03/2015.
 */
public class RequestGrantedMessage extends AbstractIdFlightControlMessage{

    private RequestMessage requestMessage;

    /**
     *
     * @param id DroneId how has granted the request.
     * @param requestMessage Original requestMessage
     */
    public RequestGrantedMessage(Long id, RequestMessage requestMessage) {
        super(id);
        this.requestMessage = requestMessage;
    }

    public RequestMessage getRequestMessage() {
        return requestMessage;
    }
}
