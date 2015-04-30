package drones.scheduler.messages.from;

/**
 * Created by Ronald on 22/04/2015.
 */
public class DroneUnassignedMessage implements SchedulerEvent{

    private long assignmentId;
    private long droneId;

    public DroneUnassignedMessage(long assignmentId, long droneId) {
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
