package drones.models.scheduler;

import akka.actor.ActorRef;
import drones.models.flightcontrol.FlightControl;
import models.Drone;

/**
 * Created by Ronald on 13/04/2015.
 */
public class Flight {

    private Long droneId;
    private Long assignmentId;
    private ActorRef flightControl;

    public Flight(Long droneId, Long assignmentId, ActorRef flightControl){
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
