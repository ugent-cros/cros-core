package drones.scheduler.messages.from;

/**
 * Created by Ronald on 16/04/2015.
 */
public class AssignmentCanceledMessage implements SchedulerEvent{
    
    private long assignmentId;

    public AssignmentCanceledMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    @Override
    public String toString() {
        return String.format("Assignment %d canceled.",assignmentId);
    }
}
