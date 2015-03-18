package drones.models.scheduler;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Assignment;
import models.Drone;

import java.util.*;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Most simple version of a scheduler.
First come, first served.
 */
public class SimpleScheduler extends Scheduler{

    //Query to fetch all assignments without progress and without assigned drone.
    private static final Query<Assignment> assignments;
    static{
        assignments = Ebean.createQuery(Assignment.class);
        assignments.where().eq("progress", 0);
        assignments.where().eq("assignedDrone",null);
        assignments.orderBy("id");
    }

    private Fleet fleet;
    private AbstractQueue<Assignment> queue;


    @Override
    protected void init(Fleet fleet) {
        this.fleet = fleet;
        this.queue = new PriorityQueue<Assignment>();
    }

    @Override
    protected void fetch(long assignmentId) {
        // Retrieve assignment from database
        Assignment assignment = Assignment.FIND.byId(assignmentId);
        // Add new assignments to the queue in order of Id
        queue.add(assignment);
        // Start scheduling
        schedule();
    }

    protected void assign(Assignment assignment, DroneCommander commander){
        Drone drone = commander.getModel();
        assignment.setAssignedDrone(drone);
        assignment.update();
        //TODO: Create FlightControl and pass the commander
    }

    @Override
    protected void schedule() {
        while(!queue.isEmpty()) {
            DroneCommander commander = findIdleCommander();
            if(commander == null) break; // No more commanders available
            Assignment assignment = queue.remove();
            assign(assignment,commander);
        }
        //No more assignments available
    }

    private DroneCommander findIdleCommander(){
        //Collection<DroneCommander> commanders = fleet.getDrones().values();
        //for(DroneCommander commander : commanders){
        //    if(commander.isIdle()){
        //        return commander;
        //    }
        //}
        // No idle commander found
        return null;
    }

    @Override
    protected void completed(Assignment assignment) {
        // Update the assignment
        assignment.setAssignedDrone(null);
        assignment.setProgress(100);
        assignment.update();
        // Maybe a drone has become available
        schedule();
    }
}
