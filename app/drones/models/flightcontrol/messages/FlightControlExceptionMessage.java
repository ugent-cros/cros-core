package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 26/03/2015.
 */
public class FlightControlExceptionMessage extends Exception implements Serializable{

    public FlightControlExceptionMessage(String s) {
        super(s);
    }

    public FlightControlExceptionMessage(Throwable throwable) {
        super(throwable);
    }
}
