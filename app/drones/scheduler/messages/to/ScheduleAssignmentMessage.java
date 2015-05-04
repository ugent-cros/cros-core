package drones.scheduler.messages.to;

import java.io.Serializable;

/**
 * Created by Ronald on 4/05/2015.
 */
public class ScheduleAssignmentMessage implements Serializable {

    private long assignmentId;

    public ScheduleAssignmentMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }
}
