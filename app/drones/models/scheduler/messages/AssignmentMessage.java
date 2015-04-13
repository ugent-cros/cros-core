package drones.models.scheduler.messages;

import models.Assignment;

import java.io.Serializable;

/**
 * Created by Ronald on 18/03/2015.
 */
public class AssignmentMessage implements Serializable{

    private Long assignmentId;

    public AssignmentMessage(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

}
