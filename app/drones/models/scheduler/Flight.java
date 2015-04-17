package drones.models.scheduler;

import akka.actor.ActorRef;

/**
 * Created by Ronald on 13/04/2015.
 */
public class Flight {

    public static final long NO_ASSIGNMENT_ID = -1;
    public static final long RETURN_HOME = -2;

    private long droneId;
    private long assignmentId;
    private ActorRef flightControl;

    public Flight(long droneId, ActorRef flightControl) {
        this(droneId, NO_ASSIGNMENT_ID, flightControl);
    }

    public Flight(long droneId, long assignmentId, ActorRef flightControl) {
        this.droneId = droneId;
        this.assignmentId = assignmentId;
        this.flightControl = flightControl;
    }

    public Long getDroneId() {
        return droneId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public ActorRef getFlightControl() {
        return flightControl;
    }
}
