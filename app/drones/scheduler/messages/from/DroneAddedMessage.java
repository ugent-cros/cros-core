package drones.scheduler.messages.from;

/**
 * Created by Ronald on 16/04/2015.
 */
public class DroneAddedMessage implements SchedulerEvent {

    private long droneId;

    public DroneAddedMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
