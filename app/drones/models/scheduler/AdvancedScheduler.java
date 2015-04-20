package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import akka.util.Timeout;
import com.avaje.ebean.Ebean;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.PingResult;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.messages.StartFlightControlMessage;
import drones.models.flightcontrol.messages.StopFlightControlMessage;
import drones.models.scheduler.messages.from.*;
import drones.models.scheduler.messages.to.*;
import models.*;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 10/04/2015.
 */
public class AdvancedScheduler extends SimpleScheduler implements Comparator<Assignment>{

    protected static final Timeout STOP_TIMEOUT = new Timeout(Duration.create(10, TimeUnit.SECONDS));
    protected Set<Long> dronePool = new HashSet<>();
    protected Map<Long, Flight> flights = new HashMap<>();

    public AdvancedScheduler() {
        queue = new PriorityQueue<>(MAX_QUEUE_SIZE,this);
    }

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        // TODO: add more receivers
        return super.initReceivers().
                match(CancelAssignmentMessage.class,
                        m -> cancelAssignment(m)
                ).
                match(AddDroneMessage.class,
                        m -> addDrone(m)
                ).
                match(RemoveDroneMessage.class,
                        m -> removeDrone(m)
                ).
                match(PublishMessage.class,
                        m -> publish(m)
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
    protected void stop(StopSchedulerMessage message) {
        // Hotswap new receive behaviour
        context().become(ReceiveBuilder
                .match(FlightCanceledMessage.class, m -> returnHome(m.getDroneId()))
                .match(DroneArrivalMessage.class, m -> termination(m.getDroneId()))
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
            context().stop(self());
        } else {
            // Cancellation
            for (Flight flight : flights.values()) {
                cancelFlight(flight);
            }
        }
    }

    /**
     * Tell the scheduler to publish a certain event message.
     * @param message containing the event
     */
    protected void publish(PublishMessage message){
        eventBus.publish(message.getEvent());
    }

    /**
     * Signal the scheduler that a drone has returned home after stop
     * Only when every active drone is home safely, we will terminate the scheduler.
     *
     * @param droneId
     */
    protected void termination(long droneId) {
        droneArrived(droneId);
        // Check if we can terminate
        if (flights.isEmpty()) {
            eventBus.publish(new SchedulerStoppedMessage());
            // Terminate the scheduler actor.
            getContext().stop(self());
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
                // Tell everyone
                eventBus.publish(new DroneAssignedMessage(drone.getId(),assignment.getId()));
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

    @Override
    protected void droneArrived(long droneId) {
        // Retrieve flight
        Flight flight = flights.remove(droneId);
        if (flight == null) {
            log.warning("[AdvancedScheduler] Received arrival from nonexistent flight.");
            return;
        }

        // Handle the flight control
        flight.getFlightControl().tell(new StopFlightControlMessage(), self());

        // Retrieve the drone
        Drone drone = getDrone(flight.getDroneId());

        // Handle the drone and assignment
        long assignmentId = flight.getAssignmentId();

        // The drone completed an assignment
        if (assignmentId > Flight.NO_ASSIGNMENT_ID) {
            Assignment assignment = getAssignment(assignmentId);
            unassign(drone, assignment);
            // We completed an assignment, yaay!
            eventBus.publish(new AssignmentCompletedMessage(assignmentId));
            // Send the drone back to base
            returnHome(drone);
            return;
        }
        // The drone becomes available again.
        if(drone.getStatus() == Drone.Status.FLYING){
            drone.setStatus(Drone.Status.AVAILABLE);
            drone.update();
            dronePool.add(drone.getId());
            // Start scheduling again because there may be assignments waiting.
            self().tell(new ScheduleMessage(), self());
            return;
        }
        // The drone was scheduled to be removed.
        if(drone.getStatus() == Drone.Status.DECOMMISSIONED){
            dronePool.remove(drone);
            drone.setStatus(Drone.Status.INACTIVE);
            drone.update();
            // Tell the world we successfully removed a drone from the pool.
            eventBus.publish(new DroneRemovedMessage(drone.getId()));
        }
    }

    /**
     * Find the closest drone with enough battery to complete the assignment.
     * @param assignment
     * @return the drone that will complete the assignment
     */
    protected Drone fetchAvailableDrone(Assignment assignment) {
        // Distance to complete the assignment route.
        double routeLength =  Helper.getRouteLength(assignment);
        if(Double.isNaN(routeLength)){
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
                dronePool.remove(drone.getId());
                drone.setStatus(Drone.Status.UNREACHABLE);
                drone.update();
                eventBus.publish(new DroneRemovedMessage(drone.getId()));
                log.warning("[AdvancedScheduler] Encountered and removed unresponsive drone.");
            }

            // Calculate distance to first checkpoint.
            double distance = Helper.distance(droneLocation, assignmentLocation);
            if(distance < minDistance){
                double totalDistance = distance + routeLength;
                if(hasSufficientBattery(commander,totalDistance)){
                    minDistance = distance;
                    minDrone = drone;
                }
            }

        }
        // Remove fetched drone from the drone pool.
        if(minDrone != null){
            dronePool.remove(minDrone.getId());
        }
        return minDrone;
    }

    protected Drone getDrone(long droneId) {
        return Drone.FIND.byId(droneId);
    }

    protected Assignment getAssignment(long assignmentId) {
        return Assignment.FIND.byId(assignmentId);
    }

    /**
     * Retrieve location of a drone via his commander.
     * @param commander
     * @return
     */
    protected Location getDroneLocation(DroneCommander commander){
        // Make sure we have a commander
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

    /**
     * Retrieve the commander for a drone.
     * If no commander was found, the drone will be removed from the drone pool.
     * @param drone
     * @return
     */
    protected DroneCommander getCommander(Drone drone){
        Fleet fleet = Fleet.getFleet();
        if(fleet.hasCommander(drone)){
            return fleet.getCommanderForDrone(drone);
        }else{
            log.warning("[AdvancedScheduler] Found drone without commander.");
            return null;
        }
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

    /**
     * Cancel an assignment, regardless whether it is in progress or not.
     * @param message containing the assignment id
     */
    protected void cancelAssignment(CancelAssignmentMessage message) {
        Assignment assignment = getAssignment(message.getAssignmentId());
        // Assigned drone
        Drone drone = assignment.getAssignedDrone();
        if (drone != null) {
            // Cancel flight
            cancelFlight(drone.getId());
        }
        // Tell the world we successfully canceled this assignment.
        eventBus.publish(new AssignmentCanceledMessage(message.getAssignmentId()));
    }

    /**
     * Add a new drone to the drone pool.
     * @param message containing the add-drone request
     */
    protected void addDrone(AddDroneMessage message) {
        Drone drone = getDrone(message.getDroneId());
        Fleet fleet = Fleet.getFleet();

        // Executor for OnCompletes.
        ExecutionContextExecutor executor = getContext().dispatcher();
        // If ping completes
        OnComplete<PingResult> pingComplete = new OnComplete<PingResult>() {
            @Override
            public void onComplete(Throwable failure, PingResult result) throws Throwable {
                boolean success = (failure == null) && (result == PingResult.OK);
                if(success){
                    // Add drone to the pool
                    dronePool.add(message.getDroneId());
                }
                SchedulerEvent event = new DroneAddedMessage(drone.getId(),success);
                self().tell(new PublishMessage(event),ActorRef.noSender());
            }
        };
        // Create a commander for this drone
        if(!fleet.hasCommander(drone)){
            OnComplete<DroneCommander> commanderComplete = new OnComplete<DroneCommander>(){
                @Override
                public void onComplete(Throwable failure, DroneCommander commander) throws Throwable {
                    boolean success = (failure == null) && (commander != null);
                    if(success){
                        Future<PingResult> pingFuture = fleet.isReachable(drone);
                        pingFuture.onComplete(pingComplete,executor);
                    }else{
                        SchedulerEvent event = new DroneAddedMessage(drone.getId(),false);
                        self().tell(new PublishMessage(event), ActorRef.noSender());
                    }
                }
            };
            Future<DroneCommander> futureCommander = fleet.createCommanderForDrone(drone);
            futureCommander.onComplete(commanderComplete, executor);
        }
    }

    /**
     * Remove a drone from the drone pool or flights in progress.
     * @param message containing the drone id
     */
    protected void removeDrone(RemoveDroneMessage message) {
        long droneId = message.getDroneId();
        if (dronePool.contains(droneId)) {
            // Drone present in drone pool is safe to remove.
            dronePool.remove(droneId);
            // Tell the world we successfully removed a drone from the drone pool.
            eventBus.publish(new DroneRemovedMessage(droneId));
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
     * Create a flight associated with an assignment.
     * Also notify subscribers that the assignment has started.
     * @param drone
     * @param assignment
     */
    protected void createFlight(Drone drone, Assignment assignment) {
        createFlight(drone.getId(), assignment.getId(), assignment.getRoute());
        // Tell the world that this assignment has started
        eventBus.publish(new AssignmentStartedMessage(assignment.getId()));
    }

    /**
     * Create a flight with a drone, an assignment id and the route to fly.
     * @param droneId
     * @param assignmentId
     * @param route
     */
    protected void createFlight(long droneId, long assignmentId, List<Checkpoint> route) {
        // TODO: Create a new Flight and add it to flights.
        // TODO: Tell a controltower to start a new flight.
        // For now, we still use SimplePilot.
        // Create SimplePilot
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), droneId, false, route)));
        // Tell the pilot to start the flight
        pilot.tell(new StartFlightControlMessage(), self());
        // Create a new flight for administration
        Flight flight = new Flight(droneId, assignmentId, pilot);
    }

    /**
     * Cancel flight of a drone if any.
     *
     * @param droneId id of the drone whose flight will be canceled
     */
    protected void cancelFlight(long droneId) {
        cancelFlight(flights.get(droneId));
    }

    /**
     * Cancel flight in progress.
     *
     * @param flight to cancel
     */
    protected void cancelFlight(Flight flight) {
        if (flight == null) {
            log.warning("[Advanced Scheduler] Tried to cancel nonexistent flight.");
            return;
        }
        // Handle assignment victim
        long assignmentId = flight.getAssignmentId();
        // Check if this flight is associated with an actual assignment
        if (assignmentId > Flight.NO_ASSIGNMENT_ID) {
            Assignment assignment = getAssignment(assignmentId);
            assignment.setAssignedDrone(null);
            assignment.update();
        }

        // Handle flightControl
        // TODO: Change this to work with controltower
        flight.getFlightControl().tell(new StopFlightControlMessage(),self());

    }

    /**
     * Creates a flight that sends the drone back to the nearest basestation.
     *
     * @param droneId id of the drone to send back to base
     */
    protected void returnHome(long droneId) {
        returnHome(getDrone(droneId));
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
        Basestation station = Helper.closestBaseStation(droneLocation);
        if (station == null) {
            log.error("[AdvancedScheduler] Found no basestations.");
            return;
        }
        Location location = station.getLocation();
        createFlight(drone.getId(), Flight.RETURN_HOME, Helper.routeTo(location));
    }
}
