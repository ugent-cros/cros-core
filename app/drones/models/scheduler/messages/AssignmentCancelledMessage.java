package drones.models.scheduler.messages;

/**
 * Created by Ronald on 13/04/2015.
 */
public class AssignmentCancelledMessage {

    private long assignmentId;

    public AssignmentCancelledMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }
}
