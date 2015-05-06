package drones.scheduler.messages.from;

/**
 * Created by Ronald on 21/04/2015.
 */
public class DroneFailedMessage implements SchedulerEvent{

    private long droneId;
    private String reason;

    public DroneFailedMessage(long droneId, String reason) {
        this.droneId = droneId;
        this.reason = reason;
    }

    public long getDroneId() {
        return droneId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("Drone %d failed: %s", droneId, reason);
    }
}
