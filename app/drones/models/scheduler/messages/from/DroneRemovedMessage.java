package drones.models.scheduler.messages.from;

import java.io.Serializable;

/**
 * Created by Ronald on 16/04/2015.
 */
public class DroneRemovedMessage implements SchedulerEvent{

    private long droneId;

    public DroneRemovedMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}