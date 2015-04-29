package drones.models.scheduler.messages.from;

import java.io.Serializable;

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
