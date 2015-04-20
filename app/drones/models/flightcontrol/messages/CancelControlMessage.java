package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Message to cancel a flightcontrol
 *
 * Created by Sander on 16/04/2015.
 */
public class CancelControlMessage implements Serializable {

    private Long droneId;

    public CancelControlMessage(Long droneId) {
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}
