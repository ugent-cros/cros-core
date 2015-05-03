package drones.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.dispatch.OnComplete;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import droneapi.api.DroneCommander;
import drones.flightcontrol.SimplePilot;
import drones.flightcontrol.messages.StartFlightControlMessage;
import drones.flightcontrol.messages.StopFlightControlMessage;
import drones.flightcontrol.messages.WayPointCompletedMessage;
import drones.models.Fleet;
import drones.scheduler.messages.from.*;
import drones.scheduler.messages.to.*;
import models.*;
import play.Logger;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 10/04/2015.
 */
public class AdvancedScheduler extends Scheduler implements Comparator<Assignment> {

    // Temporary metric in battery percentage per meter.
    // Every meter, this drone uses 0.01% of his total power, so he can fly 10km.
    public static final float BATTERY_PERCENTAGE_PER_METER = 0.01f;
    private static final Duration TIMEOUT = Duration.create(2, TimeUnit.SECONDS);
    private static final int MAX_QUEUE_SIZE = 100;
    private Set<Long> dronePool = new HashSet<>();
    private Map<Long, Flight> flights = new HashMap<>();
    private Queue<Assignment> queue;

    public AdvancedScheduler() {
        queue = new PriorityQueue<>(MAX_QUEUE_SIZE, this);
    }

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        // TODO: add more flight control receivers
        return super.initReceivers()
                .match(DroneAddedMessage.class, m -> droneAdded(m))
                .match(WayPointCompletedMessage.class, m -> waypointCompleted(m))
                .match(FlightCanceledMessage.class, m -> flightCanceled(m))
                .match(FlightCompletedMessage.class, m -> flightCompleted(m));
    }

    @Override
    public int compare(Assignment a1, Assignment a2) {
        if (a1.getPriority() == a2.getPriority()) {
            return Long.compare(a1.getId(), a2.getId());
        } else {
            return Integer.compare(a1.getPriority(), a2.getPriority());
        }
    }

    @Override
    protected void stop(StopSchedulerMessage message) {
        // Hotswap new receive behaviour
        context().become(ReceiveBuilder
                .match(CancelAssignmentMessage.class, m -> cancelAssignment(m))
                .match(FlightCanceledMessage.class, m -> flightCanceled(m))
                .match(FlightCompletedMessage.class, m -> termination(m))
                .matchAny(m -> log.warning("[AdvancedScheduler] Termination ignored message: [{}]", m.getClass().getName())
                ).build());

        // Deschedule all assignments in the queue
        for (Assignment assignment : queue) {
            assignment.setScheduled(false);
        }
        Ebean.save(queue);

        // Cancel all remaining flights
        if (flights.isEmpty()) {
            // Termination
            eventBus.publish(new SchedulerStoppedMessage());
            Logger.debug("STOPPED");
            context().stop(self());
        } else {
            // Cancellation
            for (Flight flight : flights.values()) {
                if (flight.getType() == Flight.Type.ASSIGNMENT) {
                    Scheduler.cancelAssignment(flight.getAssignmentId());
                }
                if (flight.getType() == Flight.Type.RETURN) {
                    cancelFlight(flight);
                }
            }
        }
    }

    /**
     * Signal the scheduler that a drone has returned home after stop
     * Only when every active drone is home safely, we will terminate the scheduler.
     *
     * @param message
     */
    protected void termination(FlightCompletedMessage message) {
        flightCompleted(message);
        // Check if we can terminate
        if (flights.isEmpty()) {
            eventBus.publish(new SchedulerStoppedMessage());
            // Terminate the scheduler actor.
            getContext().stop(self());
        }
    }

    @Override
    protected void setDroneEmergency(DroneEmergencyMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.error("Could not found drone for emergency: " + message.getDroneId());
        }
        if (flights.containsKey(drone.getId())) {
            cancelFlight(drone, Drone.Status.EMERGENCY);
        } else {
            // No registered flight
            // Try to land anyway
            // TODO: Do this elsewhere.
            Fleet fleet = Fleet.getFleet();
            if (fleet.hasCommander(drone)) {
                DroneCommander commander = fleet.getCommanderForDrone(drone);
                commander.land();
            }
            updateDroneStatus(drone, Drone.Status.EMERGENCY);
        }
    }

    @Override
    protected void schedule(ScheduleMessage message) {
        // Create second queue for assignments that can't be assigned right now.
        Queue<Assignment> unassigned = new PriorityQueue<>(MAX_QUEUE_SIZE, this);
        if (queue.isEmpty()) {
            // Provide assignments
            fetchAssignments();
        }
        while (!queue.isEmpty() && !dronePool.isEmpty()) {
            // Remove the assignment from the queue
            Assignment assignment = queue.remove();
            // Pick a drone
            Drone drone = fetchAvailableDrone(assignment);
            if (drone == null) {
                //There is no drone suited for this assignment
                unassigned.add(assignment);
            } else {
                assign(drone, assignment);
            }
        }
        // Refill the queue.
        if (queue.size() > unassigned.size()) {
            //
            queue.addAll(unassigned);
        } else {
            unassigned.addAll(queue);
            queue = unassigned;
        }
    }

    protected void assign(Drone drone, Assignment assignment) {
        // Update assignment
        assignment.setAssignedDrone(drone);
        assignment.update();
        eventBus.publish(new DroneAssignedMessage(assignment.getId(), drone.getId()));
        // Go flying!
        createFlight(drone, assignment);
    }

    protected void unassign(Drone drone, Assignment assignment) {
        // Update assignment
        assignment.setAssignedDrone(null);
        assignment.update();
        eventBus.publish(new DroneUnassignedMessage(assignment.getId(), drone.getId()));
    }

    protected void flightCompleted(FlightCompletedMessage message) {
        Logger.debug("COMPLETED");
        // Retrieve flight
        Flight flight = flights.remove(message.getDroneId());
        if (flight == null) {
            Logger.warn("Received arrival from nonexistent flight.");
            return;
        }
        Logger.debug("COMPLETED FLIGHT: " + flight.hashCode());
        // Stop flight control
        flight.getFlightControl().tell(new StopFlightControlMessage(), self());

        // Handle the drone and assignment
        Drone drone = getDrone(flight.getDroneId());
        // The drone completed an assignment
        if (flight.getType() == Flight.Type.ASSIGNMENT) {
            Assignment assignment = getAssignment(flight.getAssignmentId());
            unassign(drone, assignment);
            eventBus.publish(new AssignmentCompletedMessage(assignment.getId()));
        }
        // Figure out what to do next
        handleNextDroneStatus(flight, drone);
    }

    private void handleNextDroneStatus(Flight flight, Drone drone) {
        switch (flight.getNextStatus()) {
            case AVAILABLE:
                // Standard behavious if a drone returned to base
                setDroneAvailable(drone);
                break;
            case FLYING:
                // Standard behaviour if assignment completed
                returnHome(drone, Drone.Status.AVAILABLE);
                break;
            case CHARGING:
                if (flight.getType() != Flight.Type.RETURN) {
                    returnHome(drone, Drone.Status.CHARGING);
                } else {
                    updateDroneStatus(drone, Drone.Status.CHARGING);
                }
            case RETIRED:
                if (flight.getType() != Flight.Type.RETURN) {
                    // Return to base before retiring
                    returnHome(drone, Drone.Status.RETIRED);
                } else {
                    // Save to retire
                    removeDrone(drone);
                }
                break;
            default:
                updateDroneStatus(drone, flight.getNextStatus());
        }

    }

    /**
     * Update and forward progress from an assignment.
     */
    protected void waypointCompleted(WayPointCompletedMessage message) {
        Flight flight = flights.get(message.getDroneId());
        if (flight == null || flight.getType() != Flight.Type.ASSIGNMENT) {
            // There is no assignment associated to this progress
            return;
        }
        Assignment assignment = getAssignment(flight.getAssignmentId());
        if (assignment == null) {
            // Assignment was deleted
            return;
        }
        assignment.setProgress(message.getWaypointNumber());
        assignment.update();
        eventBus.publish(new AssignmentProgressedMessage(assignment.getId(), message.getWaypointNumber()));
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
     * Find the closest drone with enough battery to complete the assignment.
     *
     * @param assignment
     * @return the drone that will complete the assignment
     */
    protected Drone fetchAvailableDrone(Assignment assignment) {
        // Distance to complete the assignment route.
        double routeLength = Helper.getRouteLength(assignment);
        if (Double.isNaN(routeLength)) {
            // Encountered invalid assignment.
            Logger.error("Encountered invalid assignment route: " + assignment.getId());
            return null;
        }

        // Distance to return to base station after assignment completion
        int routeSize = assignment.getRoute().size();
        Location stopLocation = assignment.getRoute().get(routeSize - 1).getLocation();
        Basestation station = Helper.closestBaseStation(stopLocation);
        if (station != null) {
            // No return station
            routeLength += Helper.distance(stopLocation, station.getLocation());
        }

        // Find the closest drone to this assignment start location
        Location startLocation = assignment.getRoute().get(0).getLocation();
        double minDistance = Double.MAX_VALUE;
        Drone minDrone = null;
        // Consider all drones
        for (Long droneId : dronePool) {
            Drone drone = getDrone(droneId);
            if (drone == null) {
                Logger.warn("Found invalid drone id in pool: " + droneId);
                continue;
            }
            if (drone.getStatus() != Drone.Status.AVAILABLE) {
                continue;
            }
            DroneCommander commander = getCommander(drone);
            Location droneLocation = getDroneLocation(commander);
            if (droneLocation == null) {
                updateDroneStatus(drone, Drone.Status.UNREACHABLE);
                Logger.warn("Encountered an unresponsive drone: " + droneId);
                continue;
            }

            // Calculate distance to first checkpoint.
            double distance = Helper.distance(droneLocation, startLocation);
            if (distance < minDistance) {
                double totalDistance = distance + routeLength;
                if (hasSufficientBattery(commander, totalDistance)) {
                    minDistance = distance;
                    minDrone = drone;
                }
            }
        }
        return minDrone;
    }

    /**
     * Retrieve location of a drone via his commander.
     *
     * @param commander
     * @return
     */
    private Location getDroneLocation(DroneCommander commander) {
        // Make sure we have a commander
        if (commander == null) {
            Logger.warn("Can't retrieve drone location without commander.");
            return null;
        }
        // Retrieve drone location
        try {
            droneapi.model.properties.Location loc = Await.result(commander.getLocation(), TIMEOUT);
            return new Location(loc.getLatitude(), loc.getLongitude(), loc.getHeight());
        } catch (Exception ex) {
            Logger.warn("Failed to retrieve drone location.");
            return null;
        }
    }

    /**
     * Retrieve the commander for a drone.
     * If no commander was found, the drone will be removed from the drone pool.
     *
     * @param drone
     * @return
     */
    private DroneCommander getCommander(Drone drone) {
        Fleet fleet = Fleet.getFleet();
        if (fleet.hasCommander(drone)) {
            return fleet.getCommanderForDrone(drone);
        } else {
            Logger.warn("Found drone without commander: " + drone.getId());
            return null;
        }
    }

    /**
     * Decides if a drone has enough battery power left to fly a certain distance.
     *
     * @param commander of the drone
     * @param distance  to fly
     * @return true if there's enough battery power left, false otherwise
     * @throws Exception if we failed to retrieve battery status
     */
    protected boolean hasSufficientBattery(DroneCommander commander, double distance) {
        // TODO: Have a battery usage approximation for every Dronetype.
        // TODO: Take into account static battery loss and estimated travel time
        if (commander == null) {
            Logger.warn("Cannot retrieve battery status without a commander.");
            return false;
        }
        try {
            int battery = Await.result(commander.getBatteryPercentage(), TIMEOUT);
            return battery > distance * BATTERY_PERCENTAGE_PER_METER;
        } catch (Exception ex) {
            Logger.warn("Failed to retrieve battery status.");
            return false;
        }
    }

    @Override
    protected void cancelAssignment(CancelAssignmentMessage message) {
        Assignment assignment = getAssignment(message.getAssignmentId());
        // Unschedule the assignment first
        assignment.setScheduled(false);
        assignment.update();
        // Assigned drone
        Drone drone = assignment.getAssignedDrone();
        if (drone != null) {
            // Cancel flight
            cancelFlight(drone.getId());
        }
        eventBus.publish(new AssignmentCanceledMessage(message.getAssignmentId()));
    }

    @Override
    protected void addDrone(AddDroneMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("Invalid drone to add to the scheduler.");
            return;
        }
        Fleet fleet = Fleet.getFleet();
        if (!fleet.hasCommander(drone)) {
            ExecutionContextExecutor executor = getContext().dispatcher();
            OnComplete<DroneCommander> commanderComplete = new OnComplete<DroneCommander>() {
                @Override
                public void onComplete(Throwable failure, DroneCommander commander) throws Throwable {
                    boolean success = (failure == null) && (commander != null);
                    if (success) {
                        getScheduler().tell(new DroneAddedMessage(drone.getId()), self());
                    } else {
                        Scheduler.publishEvent(new DroneFailedMessage(drone.getId()));
                    }
                }
            };
            // Create commander
            fleet.createCommanderForDrone(drone).onComplete(commanderComplete, executor);
        }
    }

    private void droneAdded(DroneAddedMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.error("Received invalid droneAddedMessage: " + message.getDroneId());
            return;
        }
        dronePool.add(drone.getId());
        eventBus.publish(message);
        if (drone.getStatus() == Drone.Status.AVAILABLE) {
            Scheduler.schedule();
        }
    }

    @Override
    protected void removeDrone(RemoveDroneMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("Invalid drone to remove.");
            return;
        }
        if (!dronePool.contains(drone.getId())) {
            Logger.warn("Drone was already removed.");
            updateDroneStatus(drone, Drone.Status.RETIRED);
            return;
        }
        Flight flight = flights.get(drone.getId());
        if (flight == null) {
            // Save to remove
            dronePool.remove(drone.getId());
            removeDrone(drone);
        } else {
            flight.setNextStatus(Drone.Status.RETIRED);
            // Let flight finish if returning home.
            if (flight.getType() == Flight.Type.ASSIGNMENT) {
                cancelFlight(flight);
            }
        }
    }

    @Override
    protected void setDroneAvailable(DroneAvailableMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("Invalid drone to set available.");
            return;
        }
        if (!dronePool.contains(message.getDroneId())) {
            Logger.warn("Drone not present in drone pool.");
            updateDroneStatus(drone, Drone.Status.AVAILABLE);
            return;
        }
        if (drone.getStatus() == Drone.Status.FLYING) {
            cancelFlight(drone, Drone.Status.AVAILABLE);
        } else {
            setDroneAvailable(drone);
        }
    }

    private void setDroneAvailable(Drone drone) {
        updateDroneStatus(drone, Drone.Status.AVAILABLE);
        Scheduler.schedule();
    }

    @Override
    protected void setDroneCharging(DroneChargingMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("Invalid drone to set charging.");
            return;
        }
        if (!dronePool.contains(message.getDroneId())) {
            Logger.warn("Drone not present in drone pool.");
            updateDroneStatus(drone, Drone.Status.CHARGING);
            return;
        }
        if (drone.getStatus() == Drone.Status.FLYING) {
            cancelFlight(drone, Drone.Status.CHARGING);
        } else {
            updateDroneStatus(drone, Drone.Status.CHARGING);
        }
    }

    @Override
    protected void setDroneInactive(DroneInactiveMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("Invalid drone to set inactive.");
            return;
        }
        if (!dronePool.contains(message.getDroneId())) {
            Logger.warn("Drone not present in drone pool.");
            updateDroneStatus(drone, Drone.Status.INACTIVE);
            return;
        }
        if (drone.getStatus() == Drone.Status.FLYING) {
            cancelFlight(drone, Drone.Status.INACTIVE);
        } else {
            updateDroneStatus(drone, Drone.Status.INACTIVE);
        }
    }

    @Override
    protected void setDroneManualControl(DroneManualControlMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("Invalid drone to give manual control.");
            return;
        }
        if (!dronePool.contains(message.getDroneId())) {
            Logger.warn("Drone not present in drone pool.");
            updateDroneStatus(drone, Drone.Status.MANUAL_CONTROL);
            return;
        }
        if (drone.getStatus() == Drone.Status.FLYING) {
            cancelFlight(drone, Drone.Status.MANUAL_CONTROL);
        } else {
            updateDroneStatus(drone, Drone.Status.MANUAL_CONTROL);
        }
    }

    private void removeDrone(Drone drone) {
        // Stop commander
        boolean removed = Fleet.getFleet().stopCommander(drone);
        if (!removed) {
            Logger.warn("Tried to shut down non-existent commander.");
        }
        // Update status
        updateDroneStatus(drone, Drone.Status.RETIRED);
        eventBus.publish(new DroneRemovedMessage(drone.getId()));
        return;
    }

    /**
     * Create a flight associated with an assignment.
     * Also notify subscribers that the assignment has started.
     *
     * @param drone
     * @param assignment
     */
    private void createFlight(Drone drone, Assignment assignment) {
        long droneId = drone.getId();
        // TODO: Use ControlTower
        // Create flight control
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), droneId, false, assignment.getRoute())));
        // Record flight
        Flight flight = new Flight(droneId, assignment.getId(), pilot);
        flights.put(droneId, flight);
        Logger.debug("CREATED: " + flight.hashCode());
        updateDroneStatus(drone, Drone.Status.FLYING);
        // Start flying
        pilot.tell(new StartFlightControlMessage(), self());
        // Publish
        eventBus.publish(new AssignmentStartedMessage(assignment.getId()));
    }

    /**
     * Create a flight with a drone and the route to fly.
     *
     * @param drone
     * @param route
     */
    private void createFlight(Drone drone, List<Checkpoint> route, Drone.Status nextStatus) {
        long droneId = drone.getId();
        // TODO: Use ControlTower
        // Create flight control
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), droneId, false, route)));
        // Record flight
        Flight flight = new Flight(droneId, pilot, nextStatus);
        flights.put(droneId, flight);
        Logger.debug("CREATED: " + flight.hashCode());
        updateDroneStatus(drone, Drone.Status.FLYING);
        // Start flying
        pilot.tell(new StartFlightControlMessage(), self());
    }


    private void cancelFlight(long droneId) {
        cancelFlight(flights.get(droneId));
    }

    private void cancelFlight(Drone drone, Drone.Status cancelStatus) {
        Flight flight = flights.get(drone.getId());
        if (flight == null) {
            updateDroneStatus(drone, cancelStatus);
        } else {
            flight.setNextStatus(cancelStatus);
            cancelFlight(flight);
        }
    }

    /**
     * Cancel flight in progress.
     *
     * @param flight to cancel
     */
    private void cancelFlight(Flight flight) {
        if (flight == null) {
            Logger.warn("Tried to cancel nonexistent flight.");
            return;
        }
        // Handle associated assignment
        if (flight.getType() == Flight.Type.ASSIGNMENT) {
            flight.setType(Flight.Type.CANCELED);
            Assignment assignment = getAssignment(flight.getAssignmentId());
            Drone drone = getDrone(flight.getDroneId());
            if (assignment != null && drone != null) {
                unassign(drone, assignment);
            } else {
                Logger.error("Tried to cancel corrupt flight from drone: " + flight.getDroneId());
            }
        }
        // Ask flightcontrol to stop
        // TODO: Change this to work with controltower
        flight.getFlightControl().tell(new StopFlightControlMessage(), self());
    }

    /**
     * Creates a flight that sends the drone back to the nearest basestation.
     *
     * @param drone to send back to base
     */
    private void returnHome(Drone drone, Drone.Status nextStatus) {
        DroneCommander commander = getCommander(drone);
        Location droneLocation = getDroneLocation(commander);
        if (droneLocation == null) {
            Logger.error("Failed to send drone home.");
            updateDroneStatus(drone, Drone.Status.UNREACHABLE);
            return;
        }
        // Return to closest station
        Basestation station = Helper.closestBaseStation(droneLocation);
        if (station == null) {
            Logger.error("Found no basestations to return home.");
            return;
        }
        Location location = station.getLocation();
        createFlight(drone, Helper.routeTo(location), nextStatus);
    }

    protected void flightCanceled(FlightCanceledMessage message) {
        Logger.debug("CANCELED");
        // Retrieve associated flight
        Flight flight = flights.remove(message.getDroneId());
        if (flight == null) {
            Logger.warn("Received cancellation from nonexistent flight.");
            return;
        }
        Logger.debug("CANCELED FLIGHT: " + flight.hashCode());

        Drone drone = getDrone(flight.getDroneId());
        if (drone == null) {
            Logger.error("Canceled flight contains invalid drone id.");
            return;
        }
        // Figure out what to do next
        handleNextDroneStatus(flight, drone);
    }

    /**
     * Update the drone newStatus and publish it.
     *
     * @param drone
     * @param newStatus
     */
    private void updateDroneStatus(Drone drone, Drone.Status newStatus) {
        Drone.Status oldStatus = drone.getStatus();
        drone.setStatus(newStatus);
        drone.update();
        eventBus.publish(new DroneStatusMessage(drone.getId(), oldStatus, newStatus));
    }

    /**
     * Get drone from database, update the drone status and publish it.
     *
     * @param droneId
     * @param status
     */
    private void updateDroneStatus(long droneId, Drone.Status status) {
        updateDroneStatus(getDrone(droneId), status);
    }

}
