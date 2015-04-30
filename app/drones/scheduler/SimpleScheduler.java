package drones.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.UnitPFBuilder;
import akka.util.Timeout;
import droneapi.api.DroneCommander;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import drones.models.Fleet;
import drones.flightcontrol.SimplePilot;
import drones.flightcontrol.messages.StartFlightControlMessage;
import drones.scheduler.messages.to.*;
import models.Assignment;
import models.Checkpoint;
import models.Drone;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Most simple version of a scheduler.
First come, first served...
-> Assignment with lowest Id first.
 */
public class SimpleScheduler extends Scheduler {

    // Minimum battery percentage a drone needs to get an assignment
    private static final int MIN_BATTERY_PERCENTAGE = 50;
    // Maximum waiting time for a commander to answer
    protected static final Duration TIMEOUT = Duration.create(2, TimeUnit.SECONDS);


    // Limited queue size to prevent to many assignments in memory
    protected static final int MAX_QUEUE_SIZE = 100;
    protected Queue<Assignment> queue;
    // [QUICK FIX]
    protected Map<Long,ActorRef> flights = new HashMap<>();

    public SimpleScheduler() {
        this.queue = new PriorityQueue<>((a1, a2) -> Long.compare(a1.getId(), a2.getId()));
    }

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        return super.initReceivers()
                .match(AssignmentMessage.class, m -> receiveAssignmentMessage(m));
    }

    protected void receiveAssignmentMessage(AssignmentMessage message) {
        // Start scheduling
        schedule(null);
    }

    @Override
    protected void emergency(EmergencyMessage message) {
        long droneId = message.getDroneId();
        ActorRef pilot = flights.remove(droneId);
        if (pilot != null) {
            // Force pilot to stop immediately
            getContext().stop(pilot);
        }

        // Find drone commander
        Drone drone = Drone.FIND.byId(droneId);
        Fleet fleet = Fleet.getFleet();
        if (fleet.hasCommander(drone)) {
            // Land immediately
            DroneCommander commander = fleet.getCommanderForDrone(drone);
            commander.land();
        }

        // Update drone
        drone.setStatus(Drone.Status.EMERGENCY);
        drone.update();
        // Update assignment
        if (pilot != null) {
            Assignment assignment = findAssignmentByDrone(drone);
            assignment.setAssignedDrone(null);
            // All assignment progress is lost
            assignment.setProgress(0);
            assignment.update();
        }
    }

    protected void receiveFlightCompletedMessage(FlightCompletedMessage message) {
        // Terminate SimplePilot
        ActorRef pilot = sender();
        getContext().stop(pilot);
        // [QUICK FIX] We can delete the flight entry
        flights.remove(message.getDroneId());
        // Drone that arrived
        Drone drone = Drone.FIND.byId(message.getDroneId());
        // Assignment that has been completed
        Assignment assignment = findAssignmentByDrone(drone);
        // Unassign drone
        unassign(drone, assignment);
        // Start scheduling again
        schedule(null);
    }

    /**
     * Updates the arrival of a drone in the database
     * @param drone      drone that arrived
     * @param assignment assignment that has been completed by arrival
     */
    protected void unassign(Drone drone, Assignment assignment) {
        // Update drone
        if (drone.getStatus() == Drone.Status.FLYING) {
            // Only previous state was flying
            drone.setStatus(Drone.Status.AVAILABLE);
            drone.update();
        }
        // Update assignment
        assignment.setAssignedDrone(null);
        assignment.update();
    }

    protected void assign(Drone drone, Assignment assignment) {
        // Update drone
        drone.setStatus(Drone.Status.FLYING);
        drone.update();
        // Update assignment
        assignment.setAssignedDrone(drone);
        assignment.update();
        // Get route
        List<Checkpoint> route = assignment.getRoute();
        // Create SimplePilot
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), drone.getId(), false, route)));

        // [QUICK FIX] Remember the pilot in case of emergency.
        flights.put(drone.getId(),pilot);

        // Tell the pilot to start the flight
        pilot.tell(new StartFlightControlMessage(), self());
    }


    /**
     * Simple Schedule loop.
     * Breaks when there are no more assignments in the queue and database.
     * Breaks when there are no more available drones.
     */
    protected void schedule(ScheduleMessage message) {
        while (true) {
            // Provide assignments
            if (queue.isEmpty()) {
                if (!fetchAssignments()) {
                    return; // No more assignments
                }
            }
            // Pick drone
            Drone drone = fetchAvailableDrone();
            if (drone == null) return; // No more drones available

            // Assign assignment to drone
            Assignment assignment = queue.remove();
            assign(drone, assignment);
        }
    }

    /**
     * Fetch new assignments from database and add them to the queue
     * No more than MAX_QUEUE_SIZE assignments can be put in the queue
     *
     * @return true if assignments are added to the queue
     */
    protected boolean fetchAssignments() {
        // How many assignments can we fetch?
        int count = MAX_QUEUE_SIZE - queue.size();
        // No assignments to fetch
        if (count == 0) return false;

        // Fetch 'count' first assignments with progress = 0, ordered by Id and Priority
        Query<Assignment> query = Ebean.createQuery(Assignment.class);
        query.setMaxRows(count);
        query.where().eq("scheduled", false);
        query.orderBy("priority, id");
        List<Assignment> assignments = query.findList();

        // Add to queue and set scheduled
        for (Assignment assignment : assignments) {
            queue.add(assignment);
            assignment.setScheduled(true);
            assignment.update();
        }

        // Return true if assignments added
        return !assignments.isEmpty();
    }

    /**
     * Method to find the most suitable drone for the assignment
     *
     * @return the drone chosen to complete the assignment
     */
    protected Drone fetchAvailableDrone() {
        Drone assignee = null;
        // Retrieve drones from database
        Query<Drone> query = Ebean.createQuery(Drone.class);
        query.where().eq("status", Drone.Status.AVAILABLE);
        List<Drone> drones = query.findList();

        // Choose a valid drone
        for (Drone drone : drones) {
            if (isValidDrone(drone)) {
                assignee = drone;
                break;
            }
        }
        return assignee;
    }

    /**
     * Check if this drone can be used for an assignment.
     * The drone must have a commander and sufficient battery life.
     *
     * @param drone
     * @return true if the drone is fitted for assignment
     */
    protected boolean isValidDrone(Drone drone) {
        // Create commander
        Fleet fleet = Fleet.getFleet();
        DroneCommander commander = null;
        if(!fleet.hasCommander(drone)) {
            log.info("[SimpleScheduler] Creating new commander.");
            try {
                commander = Await.result(fleet.createCommanderForDrone(drone), new Timeout(3, TimeUnit.SECONDS).duration());
            } catch(Exception ex){
                log.error(ex, "Failed to initialize drone: {}", drone);
            }
        }
        else {
            commander = fleet.getCommanderForDrone(drone);
        }

        // BATTERY CHECK
        try {
            int battery = Await.result(commander.getBatteryPercentage(), TIMEOUT);
            // SimpleScheduler will only look at a static battery threshold
            // Enhancements will be made in AdvancedScheduler
            return battery > MIN_BATTERY_PERCENTAGE;
        } catch (Exception ex) {
            log.warning("[SimpleScheduler] Failed to retrieve battery status.");
            drone.setStatus(Drone.Status.UNREACHABLE);
            drone.update();
            return false;
        }
    }

    /**
     * Retrieve the assignment that belongs to a certain drone
     *
     * @param drone
     * @return the assignment that has parameter as assigned drone
     */
    protected Assignment findAssignmentByDrone(Drone drone) {
        // Construct query
        Query<Assignment> query = Ebean.createQuery(Assignment.class);
        query.where().eq("assignedDrone", drone);
        // Find and return unique assignment
        return query.findUnique();
    }

    @Override
    protected void stop(StopSchedulerMessage message) {
        log.warning("[SimpleScheduler] Does not care about anything when stopped.");
        getContext().stop(self());
    }

    @Override
    protected void cancelAssignment(CancelAssignmentMessage message) {
        log.warning("[SimpleScheduler] CancelAssignmentMessage not supported.");
    }

    @Override
    protected void addDrone(AddDroneMessage message) {
        log.warning("[SimpleScheduler] AddDroneMessage not supported.");
    }

    @Override
    protected void removeDrone(RemoveDroneMessage message) {
        log.warning("[SimpleScheduler] RemoveDroneMessage not supported.");
    }
}
