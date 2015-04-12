package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.japi.pf.UnitPFBuilder;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.scheduler.messages.DroneArrivalMessage;
import drones.models.scheduler.messages.DroneBatteryMessage;
import drones.models.scheduler.messages.ScheduleMessage;
import models.Assignment;
import models.Checkpoint;
import models.Drone;
import models.Location;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 10/04/2015.
 */
public class AdvancedScheduler extends SimpleScheduler implements Comparator<Assignment>{

    private static final FiniteDuration SCHEDULE_INTERVAL = Duration.create(3, TimeUnit.SECONDS);
    private Cancellable scheduleTimer;
    private Map<Long,Drone> availableDrones;
    private Map<Long,Drone> unavailableDrones;

    public AdvancedScheduler() {
        queue = new PriorityQueue<>(MAX_QUEUE_SIZE,this);
        // Program the Akka Scheduler to continuously send ScheduleMessages.
        scheduleTimer = context().system().scheduler().schedule(
                Duration.Zero(),
                SCHEDULE_INTERVAL,
                new Runnable() {
                    @Override
                    public void run() {
                        self().tell(new ScheduleMessage(), self());
                    }
                },
                context().system().dispatcher()
        );
    }

    @Override
    protected UnitPFBuilder<Object> initReceivers() {
        // TODO: add more receivers
        return super.initReceivers();
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
        super.schedule(message);
    }

    protected Drone fetchAvailableDrone(Assignment assignment) {
        // Preferably use the closest drone.
        int minDistance = Integer.MAX_VALUE;
        Drone minDrone = null;
        for(Drone drone : availableDrones.values()){
            DroneCommander commander = getCommander(drone);
            if(commander == null) continue;
            try{
                // TODO: 2 locations?
                //Location droneLocation = Await.result(commander.getLocation(),TIMEOUT);
                //double dist = Location.distance()
            }catch(Exception ex){
                log.warning("[AdvancedScheduler] Encountered unresponsive drone.");
                availableDrones.remove(drone.getId());
                drone.setStatus(Drone.Status.UNREACHABLE);
                drone.save();
            }
        }
        return minDrone;
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
            availableDrones.remove(drone.getId());
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
                availableDrones.remove(drone.getId());
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
    protected boolean hasSufficientBattery(DroneCommander commander, double distance) throws Exception{
        // TODO: have some kind of approximation meter/batteryLevel for every Dronetype.
        // TODO: Take into account static battery loss and estimated travel time
        int battery = Await.result(commander.getBatteryPercentage(), TIMEOUT);
        return battery > distance * batteryUsage;
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
    protected void receiveDroneArrivalMessage(DroneArrivalMessage message) {

    }

    @Override
    protected void receiveDroneBatteryMessage(DroneBatteryMessage message) {

    }
}
