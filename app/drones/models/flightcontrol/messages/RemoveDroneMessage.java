package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 16/04/2015.
 */
public class RemoveDroneMessage implements Serializable {

    private Long droneId;

    public RemoveDroneMessage(Long droneId) {
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}