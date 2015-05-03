package drones.scheduler.messages.to;

import java.io.Serializable;

/**
 * Created by Ronald on 3/05/2015.
 */
public class DroneManualControlMessage implements Serializable {

    private long droneId;

    public DroneManualControlMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
