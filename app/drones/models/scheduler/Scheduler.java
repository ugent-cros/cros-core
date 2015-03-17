package drones.models.scheduler;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Assignment;
import models.Drone;

import java.util.List;
import java.util.Map;

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
                        match(Fleet.class, fleet -> {
                            init(fleet);
                        }).
                        match(Long.class, id -> {
                            fetch(id);
                        }).
                        match(Assignment.class, assignment -> {
                            completed(assignment);
                        }).build()
        );
    }

    protected abstract void init(Fleet fleet);

    // Fetch new assignments
    protected abstract void fetch(long assignmentId);

    // Schedule strategy
    protected abstract void schedule();

    // An assignment has completed
    protected abstract void completed(Assignment assignment);

}
