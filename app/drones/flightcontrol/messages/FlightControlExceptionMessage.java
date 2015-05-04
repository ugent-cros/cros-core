package drones.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 26/03/2015.
 */
public class FlightControlExceptionMessage extends Exception implements Serializable{

    private long droneId;

    public FlightControlExceptionMessage(String s, long droneId) {
        super(s);
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
