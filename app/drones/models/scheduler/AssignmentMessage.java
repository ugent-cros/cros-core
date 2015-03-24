package drones.models.scheduler;

import models.Assignment;

import java.io.Serializable;

/**
 * Created by Ronald on 18/03/2015.
 */
public class AssignmentMessage implements Serializable{

    private Assignment assignment;

    public AssignmentMessage(Assignment assignment) {
        this.assignment = assignment;
    }

    public Assignment getAssignment() {
        return assignment;
    }
}
