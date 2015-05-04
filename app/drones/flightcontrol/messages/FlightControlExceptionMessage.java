package drones.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 26/03/2015.
 */
public class FlightControlExceptionMessage extends AbstractIdFlightControlMessage implements Serializable{

    private String message;

    public FlightControlExceptionMessage(String s, long droneId) {
        super(droneId);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
