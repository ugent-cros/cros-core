package drones.scheduler.messages.from;

import models.Assignment;

/**
 * Created by Ronald on 5/05/2015.
 */
public class AssignmentStatusMessage implements SchedulerEvent {

    private long assignmentId;
    private Assignment.Status oldStatus;
    private Assignment.Status newStatus;

    public AssignmentStatusMessage(long assignmentId, Assignment.Status oldStatus, Assignment.Status newStatus) {
        this.assignmentId = assignmentId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public Assignment.Status getOldStatus() {
        return oldStatus;
    }

    public Assignment.Status getNewStatus() {
        return newStatus;
    }

    @Override
    public String toString() {
        return String.format("Assignment %d status changed from %s to %s.", assignmentId, oldStatus, newStatus);
    }
}
