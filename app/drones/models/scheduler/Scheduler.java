package drones.models.scheduler;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import drones.models.Fleet;
import models.Assignment;
import models.Drone;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Class to schedule assignments.
Accepts:
-Fleet: gives a collection of drones to use.
-Long: tells the scheduler to fetch the assignment with this id
-Assignment: return this assignment to mark it as completed
 */
public abstract class Scheduler extends AbstractActor {


    protected final Fleet fleet = Fleet.getFleet();

    public Scheduler(){
        //Receive behaviour
        receive(ReceiveBuilder.
                        match(AssignmentMessage.class, message -> {
                            fetchAssignment(message);
                        }).
                        match(DroneArrivalMessage.class, message -> {
                            droneArrival(message);
                        }).build()
        );
    }

    /**
     * Updates the dispatch in the database.
     * @param drone dispatched drone
     * @param assignment assigned assignment
     */
    protected void storeDispatch(Drone drone, Assignment assignment){
        // Update drone
        drone.setStatus(Drone.Status.UNAVAILABLE);
        drone.update();
        // Update assignment
        assignment.setAssignedDrone(drone);
        assignment.update();
    }

    /**
     * Updates the arrival of a drone in the database
     * @param drone drone that arrived
     * @param assignment assignment that has been completed by arrival
     */
    protected void storeArrival(Drone drone, Assignment assignment){
        // Update drone
        drone.setStatus(Drone.Status.AVAILABLE);
        drone.update();
        // Update assignment
        assignment.setProgress(100);
        assignment.update();
    }

    /**
     * Tell the scheduler that a new assignment is available.
     * @param message message containing the new assignment.
     */
    protected abstract void fetchAssignment(AssignmentMessage message);

    /**
     * Tell the scheduler to start scheduling
     */
    protected abstract void schedule();


    /**
     * Tell the scheduler a drone has arrived at it's destination.
     * @param message message containing the drone and it's destination.
     */
    protected abstract void droneArrival(DroneArrivalMessage message);

}
