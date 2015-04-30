package drones.flightcontrol.messages;

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
    public RequestGrantedMessage(long id, RequestMessage requestMessage) {
        super(id);
        this.requestMessage = requestMessage;
    }

    public RequestMessage getRequestMessage() {
        return requestMessage;
    }
}
