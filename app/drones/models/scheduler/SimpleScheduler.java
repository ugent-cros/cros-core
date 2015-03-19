package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.StartFlightControlMessage;
import models.Assignment;
import models.Drone;
import play.libs.Akka;

import java.util.*;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Most simple version of a scheduler.
First come, first served.
 */
public class SimpleScheduler extends Scheduler{

    // Query to fetch available drones
    private static final Query<Drone> dQuery;
    static{
        dQuery = Ebean.createQuery(Drone.class);
        dQuery.where().eq("status", Drone.Status.AVAILABLE);
    }

    private Fleet fleet;
    private AbstractQueue<Assignment> queue;
    private Map<DroneCommander,Assignment> flights;


    public SimpleScheduler(){
        this.queue = new PriorityQueue<>();
        this.flights = new HashMap<>();
    }

    @Override
    protected void fetchAssignment(AssignmentMessage message) {
        // Retrieve assignment from database
        Assignment entity = Assignment.FIND.byId(message.getAssignment().getId());
        // Add new aQuery to the queue in order of Id
        queue.add(entity);
        // Start scheduling
        schedule();
    }

    protected void assign(Drone drone, Assignment assignment){
        // Store in database
        storeDispatch(drone,assignment);
        DroneCommander commander = fleet.getCommanderForDrone(drone);
        flights.put(commander,assignment);
        // Create SimplePilot
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(drone,null)));
        // Tell the pilot to start the flight
        pilot.tell(new StartFlightControlMessage(),self());
    }

    @Override
    protected void schedule() {
        while(!queue.isEmpty()) {
            Drone drone = findAvailableDrone();
            if(drone == null) break; // No more commanders available
            Assignment assignment = queue.remove();
            assign(drone,assignment);
        }
        //No more aQuery available
    }

    private Drone findAvailableDrone(){
        List<Drone> drones = dQuery.findList();
        //TODO: Check battery status!
        // Return the first available drone
        return drones.get(1);
    }

    @Override
    protected void droneArrival(DroneArrivalMessage message) {
        DroneCommander commander = message.getCommander();
        Assignment assignment = flights.get(commander);
        //TODO: get drone
        Drone drone = null;
        storeArrival(drone,assignment);
        // Start scheduling again
        schedule();
    }
}
