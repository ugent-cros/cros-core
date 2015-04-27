package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 26/03/2015.
 */
public class AbstractIdFlightControlMessage implements Serializable {

    private Long id;

    public AbstractIdFlightControlMessage(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
