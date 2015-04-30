package drones.scheduler.messages.from;

/**
 * Created by Ronald on 21/04/2015.
 */
public class DroneFailedMessage implements SchedulerEvent{

    private long droneId;

    public DroneFailedMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
