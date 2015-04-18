package drones.models.scheduler.messages.from;

import java.io.Serializable;

/**
 * Created by Ronald on 16/04/2015.
 */
public class SchedulerRemovedDroneMessage implements SchedulerEvent{

    private long droneId;

    public SchedulerRemovedDroneMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
