package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.UnitPFBuilder;
import akka.util.Timeout;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.messages.StartFlightControlMessage;
import drones.models.scheduler.messages.from.DroneAssignedMessage;
import drones.models.scheduler.messages.to.*;
import models.Assignment;
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

    public SimpleScheduler() {
        this.queue = new PriorityQueue<>((a1, a2) -> Long.compare(a1.getId(), a2.getId()));
    }

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        return super.initReceivers().
                match(AssignmentMessage.class,
                        message -> receiveAssignmentMessage(message)
                );
    }

    protected void receiveAssignmentMessage(AssignmentMessage message) {
        // Start scheduling
        schedule(null);
    }

    @Override
    protected void droneArrived(long droneId) {
        // Terminate SimplePilot
        ActorRef pilot = sender();
        getContext().stop(pilot);
        // Drone that arrived
        Drone drone = Drone.FIND.byId(droneId);
        // Assignment that has been completed
        Assignment assignment = findAssignmentByDrone(drone);
        // Unassign drone
        unassign(drone, assignment);

        // Start scheduling again
        schedule(null);
    }

    @Override
    protected void receiveDroneBatteryMessage(DroneBatteryMessage message) {
        // Well, this is just a simple scheduler.
        // There is not much this scheduler can do about this right now
        // Except updating the database.
        Drone drone = Drone.FIND.byId(message.getDroneId());
        drone.setStatus(Drone.Status.EMERGENCY);
        drone.update();
    }

    @Override
    protected void assign(Drone drone, Assignment assignment) {
        // Store in database
        super.assign(drone, assignment);
        // Tell everyone we assigned
        eventBus.publish(new DroneAssignedMessage(assignment.getId(),drone.getId()));
        // Create a new flight.
        createFlight(drone,assignment);
    }

    @Override
    protected void emergency(EmergencyMessage message) {
        log.error("[SimpleScheduler] Emergency not implemented yet!");
    }

    /**
     * Simple Schedule loop.
     * Breaks when there are no more assignments in the queue and database.
     * Breaks when there are no more available drones.
     */
    @Override
    protected void schedule(ScheduleMessage message) {
        while (true) {
            // Provide assignments
            if (queue.isEmpty()) {
                if (!fetchAssignments()) return; // No more assignments
            }
            // Pick drone
            System.out.println("Schedules");
            Drone drone = fetchAvailableDrone();
            if (drone == null) return; // No more drones available

            // Assign assignment to drone
            Assignment assignment = queue.remove();
            assign(drone, assignment);
        }
    }

    /**
     * Terminates the simple scheduler.
     * Very unsafe when using real drones!
     * @param message
     */
    @Override
    protected void stop(StopSchedulerMessage message) {
        // This is not fully implemented in the SimpleScheduler.
        log.warning("[SimpleScheduler] Does not care what happens to the drones now.");
        log.warning("[SimpleScheduler] Use an advanced scheduler to be safe.");
        // Terminate
        context().stop(self());
    }

    protected void createFlight(Drone drone, Assignment assignment){
        // Create SimplePilot
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), drone.getId(), false, assignment.getRoute())));
        // Tell the pilot to start the flight
        pilot.tell(new StartFlightControlMessage(), self());
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

        // Fetch 'count' first assignments with progress = 0, ordered by Id
        Query<Assignment> query = Ebean.createQuery(Assignment.class);
        query.setMaxRows(count);
        query.where().eq("scheduled", false);
        query.orderBy("id");
        List<Assignment> assignments = query.findList();

        // Add assignments to the queue and update them
        for (Assignment assignment : assignments) {
            queue.add(assignment);
            // Set scheduled
            assignment.setScheduled(true);
            assignment.update();
        }

        // Only if added return true
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
        // FIND OR CREATE COMMANDER
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

        if (commander == null) {
            // Fleet was unable to create a driver
            drone.setStatus(Drone.Status.MISSING_DRIVER);
            drone.update();
            return false;
        }

        // INITIALIZE COMMANDER
        // TODO: Move the commander init to somewhere else
        if (!commander.isInitialized()) {
            try {
                Await.result(commander.init(), TIMEOUT);
            } catch (Exception ex) {
                log.warning("[SimpleScheduler] Failed to initialize commander.");
                drone.setStatus(Drone.Status.UNREACHABLE);
                drone.update();
                return false;
            }
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

}
