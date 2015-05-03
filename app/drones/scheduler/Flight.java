package drones.scheduler;

import akka.actor.ActorRef;
import models.Drone;

/**
 * Created by Ronald on 13/04/2015.
 */
public class Flight {

    public static final long NO_ASSIGNMENT_ID = -1;

    private long droneId;
    private long assignmentId;
    private ActorRef flightControl;
    private Type type;
    private Drone.Status nextStatus;

    public Flight(long droneId, long assignmentId, ActorRef flightControl) {
        this.droneId = droneId;
        this.assignmentId = assignmentId;
        this.flightControl = flightControl;
        this.type = Type.ASSIGNMENT;
        this.nextStatus = Drone.Status.FLYING;
    }

    public Flight(long droneId, ActorRef flightControl, Drone.Status nextStatus){
        this.droneId = droneId;
        this.assignmentId = NO_ASSIGNMENT_ID;
        this.flightControl = flightControl;
        this.type = Type.RETURN;
        this.nextStatus = nextStatus;
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

    public void setType(Type type) {
        this.type = type;
    }

    public Drone.Status getNextStatus() {
        return nextStatus;
    }

    public void setNextStatus(Drone.Status nextStatus) {
        this.nextStatus = nextStatus;
    }

    public enum Type{
        ASSIGNMENT,
        RETURN,
        CANCELED;
    }
}
