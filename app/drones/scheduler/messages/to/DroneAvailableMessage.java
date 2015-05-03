package drones.scheduler.messages.to;

import java.io.Serializable;

/**
 * Created by Ronald on 3/05/2015.
 */
public class DroneAvailableMessage implements Serializable {

    private long droneId;

    public DroneAvailableMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
