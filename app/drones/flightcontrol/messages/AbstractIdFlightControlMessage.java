package drones.flightcontrol.messages;

import java.io.Serializable;

/**
 * Abstract message with a droneId.
 *
 * Created by Sander on 26/03/2015.
 */
public class AbstractIdFlightControlMessage implements Serializable {

    private long id;

    public AbstractIdFlightControlMessage(long id) {
        this.id = id;
    }

    public long getDroneId() {
        return id;
    }
}
