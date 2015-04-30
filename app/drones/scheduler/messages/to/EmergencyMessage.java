package drones.scheduler.messages.to;

import java.io.Serializable;

/**
 * Created by Ronald on 21/04/2015.
 */
public class EmergencyMessage implements Serializable {

    private long droneId;

    public EmergencyMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
