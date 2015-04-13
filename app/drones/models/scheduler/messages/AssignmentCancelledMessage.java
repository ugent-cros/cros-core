package drones.models.scheduler.messages;

/**
 * Created by Ronald on 13/04/2015.
 */
public class AssignmentCancelledMessage {

    private Long assignmentId;

    public AssignmentCancelledMessage(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }
}
