package drones.models.scheduler;

import models.Assignment;

/**
 * Created by Ronald on 18/03/2015.
 */
public class AssignmentMessage {

    private Assignment assignment;

    public AssignmentMessage(Assignment assignment) {
        this.assignment = assignment;
    }

    public Assignment getAssignment() {
        return assignment;
    }
}
