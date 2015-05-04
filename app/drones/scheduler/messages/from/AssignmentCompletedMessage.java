package drones.scheduler.messages.from;

/**
 * Created by Ronald on 17/04/2015.
 */
public class AssignmentCompletedMessage implements SchedulerEvent {

    private long assignmentId;

    public AssignmentCompletedMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    @Override
    public String toString() {
        return String.format("Assignment %d completed.",assignmentId);
    }
}
