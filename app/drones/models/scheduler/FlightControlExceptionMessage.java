package drones.models.scheduler;

import java.io.Serializable;

/**
 * Created by Sander on 10/04/2015.
 */
public class FlightControlExceptionMessage extends Exception implements Serializable {

    public FlightControlExceptionMessage(String s) {
        super(s);
    }
}
