package drones.models.scheduler.messages.from;

import java.io.Serializable;

/**
 * Created by Ronald on 16/04/2015.
 */
public class SchedulerCanceledMessage implements Serializable{
    
    private long assignmentId;

    public SchedulerCanceledMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }
}
