package drones.models.scheduler.messages.from;

/**
 * Created by Ronald on 16/04/2015.
 */
public class SchedulerAssignedMessage {

    private long assignmentId;
    private long droneId;

    public SchedulerAssignedMessage(long assignmentId, long droneId) {
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
