package drones.models.scheduler.messages.from;

/**
 * Created by Ronald on 16/04/2015.
 */
public class DroneAssignedMessage implements SchedulerEvent{

    private long assignmentId;
    private long droneId;

    public DroneAssignedMessage(long assignmentId, long droneId) {
        this.assignmentId = assignmentId;
        this.droneId = droneId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public long getDroneId() {
        return droneId;
    }
}
