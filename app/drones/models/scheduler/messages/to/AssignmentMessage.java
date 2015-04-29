package drones.models.scheduler.messages.to;

import models.Assignment;

import java.io.Serializable;

/**
 * Created by Ronald on 18/03/2015.
 */
@Deprecated
public class AssignmentMessage implements Serializable{

    private long assignmentId;

    public AssignmentMessage(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

}
