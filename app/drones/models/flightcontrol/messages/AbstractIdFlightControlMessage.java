package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
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
