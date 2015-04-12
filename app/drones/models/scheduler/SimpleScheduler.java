package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import drones.models.DroneCommander;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.messages.StartFlightControlMessage;
import drones.models.scheduler.messages.AssignmentMessage;
import drones.models.scheduler.messages.DroneArrivalMessage;
import drones.models.scheduler.messages.DroneBatteryMessage;
import models.Assignment;
import models.Checkpoint;
import models.Drone;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
    private static final Duration TIMEOUT = Duration.create(10, TimeUnit.SECONDS);


    // Limited queue size to prevent to many assignments in memory
    private static final int MAX_QUEUE_SIZE = 25;
    private Queue<Assignment> queue;

    public SimpleScheduler() {
        this.queue = new PriorityQueue<>((a1, a2) -> Long.compare(a1.getId(), a2.getId()));
    }

    @Override
    protected void receiveAssignmentMessage(AssignmentMessage message) {
        // Start scheduling
        schedule();
    }

    @Override
    protected void receiveDroneArrivalMessage(DroneArrivalMessage message) {
        // Terminate SimplePilot
        ActorRef pilot = sender();
        getContext().stop(pilot);
        // Drone that arrived
        Drone drone = Drone.FIND.byId(message.getDroneId());
        // Assignment that has been completed
        Assignment assignment = findAssignmentByDrone(drone);
        // Unassign drone
        relieve(drone, assignment);

        // Start scheduling again
        schedule();
    }

    @Override
    protected void receiveDroneBatteryMessage(DroneBatteryMessage message) {
        // Well, this is just a simple scheduler.
        // There is not much this scheduler can do about this right now
        // Except updating the database.
        Drone drone = Drone.FIND.byId(message.getDroneId());
        drone.setStatus(Drone.Status.EMERGENCY_LANDED);
        drone.update();
    }

    @Override
    protected void assign(Drone drone, Assignment assignment) {
        // Store in database
        super.assign(drone, assignment);
        // Get route
        List<Checkpoint> route = assignment.getRoute();
        // Create SimplePilot
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), drone.getId(), false, route)));
        // Tell the pilot to start the flight
        pilot.tell(new StartFlightControlMessage(), self());
    }


    /**
     * Schedule loop.
     * Breaks when there are no more assignments in the queue and database.
     * Breaks when there are no more available drones.
     */
    protected void schedule() {
        while (true) {
            // Provide assignments
            if (queue.isEmpty()) {
                if (!fetchAssignments()) return; // No more assignments
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

        // Fetch 'count' first assignments with progress = 0, ordered by Id
        Query<Assignment> query = Ebean.createQuery(Assignment.class);
        query.setMaxRows(count);
        query.where().eq("progress", 0);
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
        DroneCommander commander = null;
        try {
            commander = fleet.getCommanderForDrone(drone);
        } catch (IllegalArgumentException ex) {
            log.info("[SimpleScheduler] Creating new commander.");
            commander = fleet.createCommanderForDrone(drone);
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
