package drones.models.scheduler;

import akka.actor.ActorRef;

/**
 * Created by Ronald on 13/04/2015.
 */
public class Flight {

    public static final long NO_ASSIGNMENT_ID = -1;

    private long droneId;
    private long assignmentId;
    private ActorRef flightControl;
    private Type type;

    public Flight(long droneId, long assignmentId, ActorRef flightControl) {
        this.droneId = droneId;
        this.assignmentId = assignmentId;
        this.flightControl = flightControl;
        this.type = Type.ASSIGNMENT;
    }

    public Flight(long droneId, ActorRef flightControl){
        this.droneId = droneId;
        this.assignmentId = NO_ASSIGNMENT_ID;
        this.flightControl = flightControl;
        this.type = Type.RETURN;
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

    public Type getType() {
        return type;
    }

    public enum Type{
        ASSIGNMENT,
        RETURN;
    }
}
