package drones.models.scheduler;

import akka.japi.pf.UnitPFBuilder;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.scheduler.messages.to.AddDroneMessage;
import drones.models.scheduler.messages.to.CancelAssignmentMessage;
import drones.models.scheduler.messages.to.RemoveDroneMessage;
import drones.models.scheduler.messages.to.ScheduleMessage;
import models.*;
import scala.concurrent.Await;

import java.util.*;

/**
 * Created by Ronald on 10/04/2015.
 */
public class AdvancedScheduler extends SimpleScheduler implements Comparator<Assignment>{

    private Set<Long> dronePool = new HashSet<>();
    private Map<Long, Flight> flights = new HashMap<>();
    private Set<Long> assignments = new HashSet<>();

    public AdvancedScheduler() {
        queue = new PriorityQueue<>(MAX_QUEUE_SIZE,this);
    }

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        // TODO: add more receivers
        return super.initReceivers().
                match(CancelAssignmentMessage.class,
                        m -> cancelAssignment(m.getAssignmentId())
                ).
                match(AddDroneMessage.class,
                        m-> addDrone(m.getDroneId())
                ).
                match(RemoveDroneMessage.class,
                        m-> removeDrone(m.getDroneId())
                );
    }

    @Override
    public int compare(Assignment a1, Assignment a2) {
        if(a1.getPriority() == a2.getPriority()){
            return Long.compare(a1.getId(),a2.getId());
        }else{
            return Integer.compare(a1.getPriority(),a2.getPriority());
        }
    }

    @Override
    protected void schedule(ScheduleMessage message) {
        // Create second queue for assignments that can't be assigned right now.
        Queue<Assignment> unassigned = new PriorityQueue<>(MAX_QUEUE_SIZE,this);

        if (queue.isEmpty()) {
            // Provide assignments
            fetchAssignments();
        }
        while(!queue.isEmpty() && !dronePool.isEmpty()){
            // Remove the assignment from the queue
            Assignment assignment = queue.remove();
            // Pick a drone
            Drone drone = fetchAvailableDrone();
            if(drone == null){
                //There is no drone suited for this assignment
                unassigned.add(assignment);
            }else{
                assign(drone,assignment);
            }
        }
        // Refill the queue.
        if(queue.size() > unassigned.size()){
            //
            queue.addAll(unassigned);
        }else{
            unassigned.addAll(queue);
            queue = unassigned;
        }
    }

    /**
     * Find the closest drone with enough battery to complete the assignment.
     * @param assignment
     * @return the drone that will complete the assignment
     */
    protected Drone fetchAvailableDrone(Assignment assignment) {
        // Distance to complete the assignment route.
        double routeLength =  getRouteLength(assignment);
        if(routeLength < 0){
            // Encountered invalid assignment.
            log.error("[AdvancedScheduler] Encountered invalid assignment route.");
            return null;
        }
        Location assignmentLocation = assignment.getRoute().get(0).getLocation();

        // Find the closest drone to this assignment location
        double minDistance = Double.MAX_VALUE;
        Drone minDrone = null;
        // Consider all drones
        for (Long droneId : dronePool) {

            // Retrieve drone location
            Drone drone = getDrone(droneId);
            DroneCommander commander = getCommander(drone);
            Location droneLocation = getDroneLocation(commander);
            if(droneLocation == null){
                log.warning("[AdvancedScheduler] Encountered unresponsive drone.");
            }

            // Calculate distance to first checkpoint.
            double distance = Location.distance(droneLocation,assignmentLocation);
            if(distance < minDistance){
                double totalDistance = distance + routeLength;
                if(hasSufficientBattery(commander,totalDistance)){
                    minDistance = distance;
                    minDrone = drone;
                }
            }

        }
        return minDrone;
    }

    protected Drone getDrone(Long droneId) {
        return Drone.FIND.byId(droneId);
    }

    protected Assignment getAssignment(Long assignmentId) {
        return Assignment.FIND.byId(assignmentId);
    }

    protected List<Checkpoint> routeTo(Location location) {
        List<Checkpoint> route = new ArrayList<>();
        // Create new checkpoint with longitude, latitude, altitude
        route.add(new Checkpoint(location));
        return route;
    }

    protected Location getDroneLocation(DroneCommander commander){
        if (commander == null) {
            log.warning("[AdvancedScheduler] Can't retrieve drone location without commander.");
            return null;
        }
        // Retrieve drone location
        try {
            drones.models.Location loc = Await.result(commander.getLocation(), TIMEOUT);
            return new Location(loc.getLatitude(), loc.getLongitude(), loc.getHeight());
        } catch (Exception ex) {
            log.warning("[AdvancedScheduler] Failed to retrieve drone location.");
            return null;
        }
    }

    // This is ugly code, it doesn't belong here.
    protected DroneCommander getCommander(Drone drone){
        Fleet fleet = Fleet.getFleet();
        DroneCommander commander = null;
        try {
            commander = fleet.getCommanderForDrone(drone);
        } catch (IllegalArgumentException ex) {
            log.info("[AdvancedScheduler] Creating new commander.");
            commander = fleet.createCommanderForDrone(drone);
        }
        if (commander == null) {
            // Fleet was unable to create a driver
            log.warning("[AdvancedScheduler] Failed to create commander.");
            dronePool.remove(drone.getId());
            drone.setStatus(Drone.Status.MISSING_DRIVER);
            drone.update();
        }

        // INITIALIZE COMMANDER
        // TODO: Move the commander init to somewhere else! (Fleet)
        if (!commander.isInitialized()) {
            try {
                Await.result(commander.init(), TIMEOUT);
            } catch (Exception ex) {
                log.warning("[AdvancedScheduler] Failed to initialize commander.");
                dronePool.remove(drone.getId());
                drone.setStatus(Drone.Status.UNREACHABLE);
                drone.update();
                return null;
            }
        }
        return commander;
    }


    // Temporary metric in battery usage per meter.
    // Every meter, this drone uses 0.1% of his total power.
    private static final float batteryUsage = 0.1f;

    /**
     * Decides if a drone has enough battery power left to fly a certain distance.
     * @param commander of the drone
     * @param distance to fly
     * @return true if there's enough battery power left, false otherwise
     * @throws Exception if we failed to retrieve battery status
     */
    protected boolean hasSufficientBattery(DroneCommander commander, double distance){
        // TODO: have some kind of approximation meter/batteryLevel for every Dronetype.
        // TODO: Take into account static battery loss and estimated travel time
        if(commander == null){
            log.warning("[AdvancedScheduler] Can't retrieve battery status without commander.");
            return false;
        }
        try {
            int battery = Await.result(commander.getBatteryPercentage(), TIMEOUT);
            return battery > distance * batteryUsage;
        }catch(Exception ex){
            log.warning("[AdvancedScheduler] Failed to retrieve battery status.");
            return false;
        }
    }

    // TODO: Try to have route length as an assignment property
    protected double getRouteLength(Assignment assignment){
        List<Checkpoint> route = assignment.getRoute();
        if(route.isEmpty()){
            log.error("[AdvancedScheduler] Encountered assignment with empty route.");
            return -1;
        }
        double length = 0;
        Location from = route.get(0).getLocation();
        for(int c = 1; c < route.size(); c++){
            Location to = route.get(c).getLocation();
            length += Location.distance(from,to);
            from = to;
        }
        return length;
    }

    @Override
    protected void createFlight(Drone drone, List<Checkpoint> route) {
        // TODO: Create a new Flight and add it to flights.
        // TODO: Tell a controltower to start a new flight.
    }

    /**
     * Add a drone to the drone pool.
     * @param droneId
     */
    protected void addDrone(Long droneId) {
        Drone drone = getDrone(droneId);
        if (drone.getStatus() == Drone.Status.AVAILABLE) {
            dronePool.add(drone.getId());
        }
    }

    /**
     * Remove a drone from the drone pool or flights in progress.
     * @param droneId
     */
    protected void removeDrone(Long droneId) {
        if (dronePool.contains(droneId)) {
            // Drone present in drone pool is safe to remove.
            dronePool.remove(droneId);
            return;
        }
        if (flights.containsKey(droneId)) {
            // Drone is busy with assignment, so cancel the flight.
            cancelFlight(droneId);
            // Decommission the drone
            Drone drone = getDrone(droneId);
            drone.setStatus(Drone.Status.DECOMMISSIONED);
            drone.update();
            // Send the drone home
            returnHome(drone);
        }
    }

    /**
     * Cancel flight in progress.
     *
     * @param droneId to identify the right flight.
     */
    protected void cancelFlight(Long droneId) {
        // Retrieve flight
        Flight flight = flights.get(droneId);
        if (flight == null) {
            log.warning("[Advanced Scheduler] Tried to cancel nonexistent flight.");
            return;
        }
        // Handle assignment victim
        Long assignmentId = flight.getAssignmentId();
        Assignment assignment = getAssignment(assignmentId);
        assignment.setProgress(0);
        assignment.setAssignedDrone(null);
        assignment.save();

        // Handle flightControl
        // TODO: Tell flightcontrol to stop.
    }

    /**
     * Creates a flight that sends the drone back to the nearest basestation.
     * @param drone to send back to base
     */
    protected void returnHome(Drone drone) {
        DroneCommander commander = getCommander(drone);
        Location droneLocation = getDroneLocation(commander);
        if (droneLocation == null) {
            log.error("[AdvancedScheduler] Failed to send drone home.");
            dronePool.remove(drone.getId());
            drone.setStatus(Drone.Status.UNREACHABLE);
            drone.update();
            return;
        }
        // Basestation to return to should be the closest one.
        Basestation station = closestBaseStation(droneLocation);
        if (station == null) {
            log.error("[AdvancedScheduler] Found no basestations.");
            return;
        }
        Location location = station.getLocation();
        createFlight(drone, routeTo(location));
    }

    protected void cancelAssignment(Long assignmentId) {
        Assignment assignment = getAssignment(assignmentId);
        // Assigned drone
        Drone drone = assignment.getAssignedDrone();
        if (drone != null) {
            // Cancel flight
            cancelFlight(drone.getId());
            // Send drone back to home
            returnHome(drone);
        }
    }

    protected Basestation closestBaseStation(Location location) {
        List<Basestation> stations = Basestation.FIND.all();
        double minDist = Double.MAX_VALUE;
        Basestation closest = null;
        for (Basestation station : stations) {
            double dist = Location.distance(station.getLocation(), location);
            if (dist < minDist) {
                minDist = dist;
                closest = station;
            }
        }
        return closest;
    }
}
