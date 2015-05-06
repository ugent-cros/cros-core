package drones.scheduler.messages.to;

import java.io.Serializable;

/**
 * Created by Ronald on 4/05/2015.
 */
public class ScheduleDroneMessage implements Serializable {

    private long droneId;

    public ScheduleDroneMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
