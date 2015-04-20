package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 17/04/2015.
 */
public class RemoveDroneCompletedMessage implements Serializable {

    private Long droneId;

    public RemoveDroneCompletedMessage(Long droneId) {
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}
