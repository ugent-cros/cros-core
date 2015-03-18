package drones.models.scheduler;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import drones.models.Fleet;

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

    protected abstract void init(Fleet fleet);

    // Fetch new assignments
    protected abstract void fetchAssignment(AssignmentMessage message);

    // Schedule strategy
    protected abstract void schedule();

    // An assignment has completed
    protected abstract void droneArrival(DroneArrivalMessage message);

}
