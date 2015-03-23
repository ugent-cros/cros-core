package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;


import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.StartFlightControlMessage;
import models.Assignment;
import models.Checkpoint;
import models.Drone;

import java.util.*;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Most simple version of a scheduler.
First come, first served...
-> Assignment with lowest Id first.
 */
public class SimpleScheduler extends Scheduler{

    // Query to fetch available drones
    private static final Query<Drone> DRONE_QUERY;
    static{
        DRONE_QUERY = Ebean.createQuery(Drone.class);
        DRONE_QUERY.where().eq("status", Drone.Status.AVAILABLE);
    }

    // Limited queue size to prevent to many assignments in memory
    private static final int MAX_QUEUE_SIZE = 25;
    private AbstractQueue<Assignment> queue;

    public SimpleScheduler(){
        this.queue = new PriorityQueue<>((a1,a2) -> Long.compare(a1.getId(),a2.getId()));
    }

    @Override
    protected void receiveAssignmentMessage(AssignmentMessage message) {
    	// Start scheduling
        schedule();
    }
    
    @Override
    protected void receiveDroneArrivalMessage(DroneArrivalMessage message) {
    	// Drone that arrived
        Drone drone = message.getDrone();
        // Assignment that has been completed
        Assignment assignment = findAssignmentByDrone(drone);
        // Unassign drone
        unassign(drone,assignment);
        
        // Start scheduling again
        schedule();
    }

    @Override
    protected void assign(Drone drone, Assignment assignment){
        // Store in database
        super.assign(drone,assignment);
        // Get route
        List<Checkpoint> route = assignment.getRoute();
        // Create SimplePilot
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(drone,route)));
        // Tell the pilot to start the flight
        pilot.tell(new StartFlightControlMessage(),self());
    }


    /**
     * Schedule loop.
     * Breaks when there are no more assignments in the queue and database.
     * Breaks when there are no more available drones.
     */
    protected void schedule() {
    	while(true){
    		// Provide assignments
    		if(queue.isEmpty()){
    			if(!fetchAssignments()) break; // No more assignments
    		}
    		// Pick drone
            Drone drone = findAvailableDrone();
            if(drone == null) break; // No more drones available
            
            // Assign assignment to drone
            Assignment assignment = queue.remove();
            assign(drone,assignment);
        }
    }
    
    /**
     * Fetch new assignments from database and add them to the queue
     * No more than MAX_QUEUE_SIZE assignments can be put in the queue
     * @return true if assignments are added to the queue
     */
    protected boolean fetchAssignments(){
    	// How many assignments can we fetch?
    	int count = MAX_QUEUE_SIZE - queue.size();
    	// No assignments to fetch
    	if(count == 0) return false;
    	
    	// Fetch 'count' first assignments with progress = 0, ordered by Id
    	Query<Assignment> query = Ebean.createQuery(Assignment.class);
    	query.setMaxRows(count);
    	query.where().eq("progress",0);
    	query.orderBy("id");
    	List<Assignment> assignments = query.findList();
    	
    	// Add assignments to the queue and update them
    	for (Assignment assignment : assignments) {
    		queue.add(assignment);
    		// Progress = 1 means assignment is added to scheduler queue
			assignment.setProgress(1);
			assignment.update();
		}

    	// Only if added return true
    	return !assignments.isEmpty();  	
    }

    /**
     * Method to find the most suitable drone for the assignment
     * Here, the first drone find will do.
     * @return the drone chosen to complete the assignment
     */
    protected Drone findAvailableDrone(){
        List<Drone> drones = DRONE_QUERY.findList();
        //TODO: Check battery status!
        // Return the first available drone
        return drones.get(1);
    }
    
    /**
     * Retrieve the assignment that belongs to a certain drone
     * @param drone
     * @return the assignment that has parameter as assigned drone
     */
    protected Assignment findAssignmentByDrone(Drone drone){
    	// Construct query
    	Query<Assignment> query = Ebean.createQuery(Assignment.class);
    	query.where().eq("assignedDrone", drone);
    	// Find and return unique assignment
    	Assignment assignment = query.findUnique();
    	return assignment;
    }
    
}
