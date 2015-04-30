package drones.scheduler.messages.from;

/**
 * Created by Ronald on 20/04/2015.
 */
public class AssignmentStartedMessage implements SchedulerEvent {

    private long assignmentId;

    public AssignmentStartedMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }
}
