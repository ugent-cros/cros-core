package drones.scheduler;

import akka.actor.ActorRef;
import models.Drone;

/**
 * Created by Ronald on 13/04/2015.
 */
public class Flight {

    private long droneId;
    private long assignmentId;
    private ActorRef flightControl;
    private Type type;
    private Drone.Status cancelStatus;

    public Flight(long droneId, long assignmentId) {
        this.droneId = droneId;
        this.assignmentId = assignmentId;
    }

    public Flight(long droneId, long assignmentId, ActorRef flightControl) {
        this(droneId,assignmentId);
        this.flightControl = flightControl;
        this.type = Type.ASSIGNMENT;
        this.cancelStatus = Drone.Status.AVAILABLE;
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

    public Drone.Status getCancelStatus() {
        return cancelStatus;
    }

    public void setCancelStatus(Drone.Status cancelStatus) {
        this.cancelStatus = cancelStatus;
    }

    public enum Type{
        ASSIGNMENT,
        CANCELED;
    }
}
