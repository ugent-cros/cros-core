package drones.models.scheduler.messages.from;

import java.io.Serializable;

/**
 * Created by Ronald on 16/04/2015.
 */
public class DroneAddedMessage implements SchedulerEvent {

    private long droneId;
    private boolean success;

    public DroneAddedMessage(long droneId, boolean success) {
        this.droneId = droneId;
        this.success = success;
    }

    public long getDroneId() {
        return droneId;
    }

    public boolean isSuccess() {
        return success;
    }
}
