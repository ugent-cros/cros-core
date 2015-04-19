package drones.models.scheduler.messages.from;

import java.io.Serializable;

/**
 * Created by Ronald on 16/04/2015.
 */
public class SchedulerAddedDroneMessage implements SchedulerEvent {

    private long droneId;

    public SchedulerAddedDroneMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }

}
