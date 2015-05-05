package drones.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import droneapi.api.DroneCommander;
import drones.flightcontrol.SimplePilot;
import drones.flightcontrol.messages.FlightControlExceptionMessage;
import drones.flightcontrol.messages.StartFlightControlMessage;
import drones.flightcontrol.messages.StopFlightControlMessage;
import drones.flightcontrol.messages.WayPointCompletedMessage;
import drones.models.Fleet;
import drones.scheduler.messages.from.*;
import drones.scheduler.messages.to.*;
import models.*;
import play.Logger;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import javax.persistence.OptimisticLockException;
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
    private Map<Long, Flight> flights = new HashMap<>();

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        // TODO: add more flight control receivers
        return super.initReceivers()
                .match(FlightControlExceptionMessage.class, m -> flightFailed(m))
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
    protected void startScheduler(StartSchedulerMessage message) {
        // Initial scheduling
        List<Drone> drones = Drone.FIND.all();
        for(Drone drone : drones){
            if(drone.getStatus() == Drone.Status.AVAILABLE) {
                scheduleDrone(drone.getId());
            }
        }
    }

    @Override
    protected void stopScheduler(StopSchedulerMessage message) {
        // Cancel all remaining flights
        if (flights.isEmpty()) {
            // Termination
            eventBus.publish(new SchedulerStoppedMessage());
            Logger.info("Scheduler terminates.");
            context().stop(self());
            return;
        }
        // Termination phase
        // Hotswap new receive behaviour
        context().become(ReceiveBuilder
                .match(FlightControlExceptionMessage.class, m -> termination(m))
                .match(FlightCanceledMessage.class, m -> termination(m))
                .match(FlightCompletedMessage.class, m -> termination(m))
                .matchAny(m -> log.warning("Ignored message during termination: [{}]", m.getClass().getName())
                ).build());
        for (Flight flight : flights.values()) {
            if (flight.getType() == Flight.Type.ASSIGNMENT) {
                Drone drone = getDrone(flight.getDroneId());
                if(drone == null){
                    Logger.warn("Stop: drone is null.");
                }else {
                    cancelFlight(drone, Drone.Status.INACTIVE);
                }
            }
        }
    }

    protected void termination(FlightCompletedMessage message) {
        flightCompleted(message);
        // Check if we can terminate
        if (flights.isEmpty()) {
            eventBus.publish(new SchedulerStoppedMessage());
            // Terminate the scheduler actor.
            getContext().stop(self());
        }
    }

    protected void termination(FlightCanceledMessage message){
        flightCanceled(message);
        // Check if we can terminate
        if (flights.isEmpty()) {
            eventBus.publish(new SchedulerStoppedMessage());
            // Terminate the scheduler actor.
            getContext().stop(self());
        }
    }

    protected void termination(FlightControlExceptionMessage message){
        flightFailed(message);
        // Check if we can terminate
        if (flights.isEmpty()) {
            eventBus.publish(new SchedulerStoppedMessage());
            // Terminate the scheduler actor.
            getContext().stop(self());
        }
    }


    @Override
    protected void droneEmergency(DroneEmergencyMessage message) {
        Drone drone = getDrone(message.getDroneId());
        if (drone == null) {
            Logger.warn("DroneEmergency: drone is null.");
            return;
        }
        if (flights.containsKey(drone.getId())) {
            cancelFlight(drone, Drone.Status.EMERGENCY);
        } else {
            // No registered flight, try to land anyway!
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
    protected void scheduleAssignment(ScheduleAssignmentMessage message){
        Assignment assignment = getAssignment(message.getAssignmentId());
        if(assignment == null){
            Logger.warn("ScheduleAssignment: assignment is null.");
            return;
        }
        if(assignment.getStatus() != Assignment.Status.PENDING){
            Logger.warn("ScheduleAssignment: assignment is not pending.");
            return;
        }
        Drone drone = fetchDrone(assignment);
        if(drone == null){
            Logger.info("ScheduleAssignment: no drone for assignment.");
        }else{
            assign(drone,assignment);
            createFlight(drone, assignment);
        }
    }

    @Override
    protected void scheduleDrone(ScheduleDroneMessage message){
        Drone drone = getDrone(message.getDroneId());
        if(drone == null){
            Logger.warn("ScheduleDrone: drone is null.");
            return;
        }
        if(drone.getStatus() != Drone.Status.AVAILABLE){
            Logger.warn("ScheduleDrone: drone is not available.");
            return;
        }
        Assignment assignment = fetchAssignment(drone);
        if(assignment == null){
            Logger.info("ScheduleDrone: no assignment for drone.");
        }else{
            assign(drone,assignment);
            createFlight(drone, assignment);
        }

    }

    protected void assign(Drone drone, Assignment assignment) {
        // Update assignment
        boolean updated = false;
        while(!updated) {
            assignment.refresh();
            assignment.setAssignedDrone(drone);
            try {
                assignment.update();
                updated = true;
            } catch (OptimisticLockException ex) {
                Logger.warn("Assign: retry to update.");
            }
        }
        eventBus.publish(new DroneAssignedMessage(assignment.getId(), drone.getId()));
    }

    protected void unassign(Drone drone, Assignment assignment) {
        // Update assignment
        boolean updated = false;
        while(!updated) {
            assignment.refresh();
            assignment.setAssignedDrone(null);
            try {
                assignment.update();
                updated = true;
            } catch (OptimisticLockException ex) {
                Logger.warn("Unassign: retry to update.");
            }
        }
        eventBus.publish(new DroneUnassignedMessage(assignment.getId(), drone.getId()));
    }

    protected void flightCompleted(FlightCompletedMessage message) {
        // Flight
        Flight flight = flights.remove(message.getDroneId());
        if (flight == null) {
            Logger.warn("FlightCompleted: flight is null.");
            return;
        }
        // Stop flight control
        flight.getFlightControl().tell(new StopFlightControlMessage(), self());

        // Drone
        Drone drone = getDrone(flight.getDroneId());
        if(drone == null){
            Logger.warn("FlightCompleted: drone is null.");
            return;
        }
        // Assignment
        if (flight.getType() == Flight.Type.ASSIGNMENT) {
            Assignment assignment = getAssignment(flight.getAssignmentId());
            if(assignment == null){
                Logger.warn("FlightCompleted: assignment is null.");
            }else{
                unassign(drone, assignment);
                updateAssignmentStatus(assignment, Assignment.Status.COMPLETED);
                eventBus.publish(new AssignmentCompletedMessage(assignment.getId()));
            }
        }
        updateDroneStatus(drone,Drone.Status.AVAILABLE);
        scheduleDrone(drone.getId());
    }

    protected void flightCanceled(FlightCanceledMessage message) {
        // Flight
        Flight flight = flights.remove(message.getDroneId());
        if (flight == null) {
            Logger.warn("FlightCanceled: flight is null.");
            return;
        }
        // Drone
        Drone drone = getDrone(flight.getDroneId());
        if (drone == null) {
            Logger.warn("FlightCanceled: drone is null.");
            return;
        }
        updateDroneStatus(drone, flight.getCancelStatus());
        if(flight.getCancelStatus() == Drone.Status.AVAILABLE){
            scheduleDrone(drone.getId());
        }
        // Assignment
        Assignment assignment = getAssignment(flight.getAssignmentId());
        if(assignment == null){
            Logger.warn("FlightCanceled: assignment is null.");
            return;
        }
        unassign(drone, assignment);
        if(assignment.getStatus() == Assignment.Status.CANCELED){
            eventBus.publish(new AssignmentCanceledMessage(assignment.getId()));
        }else{
            updateAssignmentProgress(assignment, 0);
            updateAssignmentStatus(assignment, Assignment.Status.PENDING);
            scheduleAssignment(assignment.getId());
        }
    }

    protected void flightFailed(FlightControlExceptionMessage message){
        // Flight
        Flight flight = flights.remove(message.getDroneId());
        if(flight == null){
            Logger.warn("FlightFailed: flight is null.");
            return;
        }
        // Drone
        Drone drone = getDrone(message.getDroneId());
        if(drone == null){
            Logger.warn("FlightFailed: drone is null.");
            return;
        }
        updateDroneStatus(drone, Drone.Status.ERROR);
        eventBus.publish(new DroneFailedMessage(drone.getId(),message.getMessage()));
        //Assignment
        Assignment assignment = getAssignment(flight.getAssignmentId());
        if(assignment == null){
            Logger.warn("FlightFailed: assignment is null.");
            return;
        }
        unassign(drone,assignment);
        updateAssignmentProgress(assignment, 0);
        updateAssignmentStatus(assignment, Assignment.Status.PENDING);
        scheduleAssignment(assignment.getId());
    }

    protected void waypointCompleted(WayPointCompletedMessage message) {
        Flight flight = flights.get(message.getDroneId());
        if (flight == null) {
            Logger.warn("WaypointCompleted: flight is null.");
            return;
        }
        if(flight.getType() == Flight.Type.CANCELED){
            Logger.warn("WaypointCompleted: flight is canceled.");
            return;
        }
        Assignment assignment = getAssignment(flight.getAssignmentId());
        if (assignment == null) {
            Logger.warn("WaypointCompleted: assignment is null.");
            return;
        }
        updateAssignmentProgress(assignment, message.getWaypointNumber() + 1);
    }

    protected Assignment fetchAssignment(Drone drone){
        // Fetch
        Query<Assignment> query = Ebean.createQuery(Assignment.class);
        query.where().eq("status", Assignment.Status.PENDING);
        query.orderBy("priority, id");
        List<Assignment> assignments = query.findList();

        DroneCommander commander = getCommander(drone);
        Location droneLocation = getDroneLocation(commander);
        if (droneLocation == null) {
            updateDroneStatus(drone, Drone.Status.UNREACHABLE);
            Logger.warn("FetchAssignment: drone unreachable.");
            return null;
        }
        for(Assignment assignment : assignments){
            // Distance to complete the assignment route.
            double routeLength = Helper.getRouteLength(assignment);
            if (Double.isNaN(routeLength)) {
                Logger.warn("FetchAssignment: invalid route length.");
                continue;
            }
            Location startLocation = assignment.getRoute().get(0).getLocation();
            double distance = Helper.distance(droneLocation, startLocation);
            double totalDistance = distance + routeLength;
            if(hasSufficientBattery(commander,totalDistance)){
                return assignment;
            }
        }
        return null;
    }

    protected Drone fetchDrone(Assignment assignment) {
        // Distance to complete the assignment route.
        double routeLength = Helper.getRouteLength(assignment);
        if (Double.isNaN(routeLength)) {
            Logger.warn("FetchDrone: invalid route length.");
            return null;
        }
        // Fetch drones
        Query<Drone> query = Ebean.createQuery(Drone.class);
        query.where().eq("status", Drone.Status.AVAILABLE);
        List<Drone> drones = query.findList();
        // Find the closest drone to this assignment startScheduler location
        Location startLocation = assignment.getRoute().get(0).getLocation();
        double minDistance = Double.MAX_VALUE;
        Drone minDrone = null;
        // Consider all drones
        for (Drone drone : drones) {
            DroneCommander commander = getCommander(drone);
            Location droneLocation = getDroneLocation(commander);
            if (droneLocation == null) {
                updateDroneStatus(drone, Drone.Status.UNREACHABLE);
                Logger.warn("FetchDrone: drone unreachable.");
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

    private Location getDroneLocation(DroneCommander commander) {
        // Make sure we have a commander
        if (commander == null) {
            Logger.warn("GetDroneLocation: commander is null.");
            return null;
        }
        // Retrieve drone location
        try {
            droneapi.model.properties.Location loc = Await.result(commander.getLocation(), TIMEOUT);
            return new Location(loc.getLatitude(), loc.getLongitude(), loc.getHeight());
        } catch (Exception ex) {
            Logger.warn("GetDroneLocation: getLocation timed out.");
            return null;
        }
    }

    private DroneCommander getCommander(Drone drone) {
        Fleet fleet = Fleet.getFleet();
        if (fleet.hasCommander(drone)) {
            return fleet.getCommanderForDrone(drone);
        } else {
            Logger.warn("GetCommander: drone has no commander.");
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
            Logger.warn("HasSufficientBattery: commander is null.");
            return false;
        }
        try {
            int battery = Await.result(commander.getBatteryPercentage(), TIMEOUT);
            return battery > distance * BATTERY_PERCENTAGE_PER_METER;
        } catch (Exception ex) {
            Logger.warn("HasSufficientBattery: getBatteryPercentage timed out.");
            return false;
        }
    }

    @Override
    protected void cancelAssignment(CancelAssignmentMessage message) {
        // Assignment
        Assignment assignment = getAssignment(message.getAssignmentId());
        if(assignment == null){
            Logger.warn("CancelAssignment: assignment == null.");
            return;
        }
        // Drone
        Drone drone = assignment.getAssignedDrone();
        if (drone == null) {
            Logger.warn("CancelAssignment: drone == null.");
            return;
        }
        cancelFlight(drone, Drone.Status.AVAILABLE);
    }

    private void createFlight(Drone drone, Assignment assignment) {
        long droneId = drone.getId();
        // Flight control
        // TODO: Use ControlTower
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), droneId, false, assignment.getRoute())));
        pilot.tell(new StartFlightControlMessage(), self());
        // Flight
        Flight flight = new Flight(droneId, assignment.getId(), pilot);
        flights.put(droneId, flight);
        updateDroneStatus(drone, Drone.Status.FLYING);
        updateAssignmentStatus(assignment, Assignment.Status.EXECUTING);
        eventBus.publish(new AssignmentStartedMessage(assignment.getId()));
    }

    private void cancelFlight(Drone drone, Drone.Status cancelStatus) {
        // Flight
        Flight flight = flights.get(drone.getId());
        if (flight == null) {
            Logger.warn("CancelFlight: flight is null");
            return;
        }
        if (flight.getType() == Flight.Type.CANCELED) {
            Logger.warn("CancelFlight: flight already canceled.");
            return;
        }
        flight.setType(Flight.Type.CANCELED);
        flight.setCancelStatus(cancelStatus);
        // Flight control
        // TODO: Use ControlTower
        flight.getFlightControl().tell(new StopFlightControlMessage(), self());
    }

    private void updateDroneStatus(Drone drone, Drone.Status newStatus) {
        boolean updated = false;
        while(!updated) {
            drone.refresh();
            Drone.Status oldStatus = drone.getStatus();
            drone.setStatus(newStatus);
            try {
                drone.update();
                updated = true;
                eventBus.publish(new DroneStatusMessage(drone.getId(), oldStatus, newStatus));
            } catch (OptimisticLockException ex) {
                Logger.warn("UpdateDroneStatus: retry to update.");
            }
        }
    }

    private void updateAssignmentStatus(Assignment assignment, Assignment.Status newStatus){
        boolean updated = false;
        while(!updated) {
            assignment.refresh();
            Assignment.Status oldStatus = assignment.getStatus();
            assignment.setStatus(newStatus);
            try {
                assignment.update();
                updated = true;
                eventBus.publish(new AssignmentStatusMessage(assignment.getId(), oldStatus, newStatus));
            } catch (OptimisticLockException ex) {
                Logger.warn("UpdateAssignmentStatus: retry to update.");
            }
        }
    }

    private void updateAssignmentProgress(Assignment assignment, int progress){
        assignment.setProgress(progress);
        try {
            assignment.update();
        }catch (OptimisticLockException ex){
            Logger.warn("UpdateAssignmentProgress: failed to update.");
            return;
        }
        eventBus.publish(new AssignmentProgressedMessage(assignment.getId(), progress));
    }
}
