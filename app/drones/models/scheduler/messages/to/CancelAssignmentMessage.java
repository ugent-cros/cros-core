package drones.models.scheduler.messages.to;

/**
 * Created by Ronald on 13/04/2015.
 */
public class CancelAssignmentMessage {

    private long assignmentId;

    public CancelAssignmentMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }
}
