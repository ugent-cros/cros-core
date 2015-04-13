package drones.models.scheduler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.models.scheduler.messages.DroneArrivalMessage;
import drones.models.scheduler.messages.DroneBatteryMessage;
import drones.models.scheduler.messages.ScheduleMessage;
import models.Assignment;
import models.Drone;
import play.libs.Akka;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Class to schedule assignments.
Accepts:
-Fleet: gives a collection of drones to use.
-Long: tells the scheduler to fetch the assignment with this id
-Assignment: return this assignment to mark it as completed
 */
public abstract class Scheduler extends AbstractActor {


    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private static ActorRef scheduler;
    private static Object lock = new Object();

    public Scheduler() {
        //Receive behaviour
        UnitPFBuilder<Object> builder = initReceivers();
        builder.matchAny(m -> log.warning("[Scheduler] Received unknown message: [{}]", m.getClass().getName()));
        receive(builder.build());
    }

    /**
     * Get an actor reference to the drone scheduler
     *
     * @return
     */
    public static ActorRef getScheduler() throws SchedulerException {
        synchronized (lock) {
            if (scheduler == null || scheduler.isTerminated()) {
                throw new SchedulerException("The scheduler has not been started yet.");
            } else {
                return scheduler;
            }
        }
    }

    public static void start(Class<? extends Scheduler> type) throws SchedulerException {
        synchronized (lock) {
            if (scheduler == null || scheduler.isTerminated()) {
                scheduler = Akka.system().actorOf(Props.create(type));
            } else {
                throw new SchedulerException("The scheduler has already been started.");
            }
        }
    }

    public static void stop() throws SchedulerException {
        synchronized (lock) {
            if (scheduler != null) {
                if (scheduler.isTerminated()) {
                    Akka.system().stop(scheduler);
                }
                scheduler = null;
            } else {
                throw new SchedulerException("The scheduler cannot be stopped before it has started.");
            }
        }
    }

    protected UnitPFBuilder<Object> initReceivers() {
        return ReceiveBuilder.
                match(ScheduleMessage.class,
                        m -> schedule(m)
                ).
                match(DroneArrivalMessage.class,
                        message -> receiveDroneArrivalMessage(message)
                ).
                match(DroneBatteryMessage.class,
                        message -> receiveDroneBatteryMessage(message)
                );
    }

    /**
     * Updates the dispatch in the database.
     *
     * @param drone      dispatched drone
     * @param assignment assigned assignment
     */
    protected void assign(Drone drone, Assignment assignment) {
        // Update drone
        drone.setStatus(Drone.Status.UNAVAILABLE);
        drone.update();
        // Update assignment
        assignment.setAssignedDrone(drone);
        assignment.update();
    }

    /**
     * Updates the arrival of a drone in the database
     *
     * @param drone      drone that arrived
     * @param assignment assignment that has been completed by arrival
     */
    protected void relieve(Drone drone, Assignment assignment) {
        // Update drone
        if (drone.getStatus() == Drone.Status.UNAVAILABLE) {
            // Set state available again if possible
            drone.setStatus(Drone.Status.AVAILABLE);
            drone.update();
        }
        // Update assignment
        assignment.setAssignedDrone(null);
        assignment.setProgress(100);
        assignment.update();
    }

    /**
     * Force the scheduler to execute schedule procedure.
     * @param message containing sequenceId
     */
    protected abstract void schedule(ScheduleMessage message);

    /**
     * Tell the scheduler a drone has arrived at it's destination.
     * @param message message containing the drone and it's destination.
     */
    protected abstract void receiveDroneArrivalMessage(DroneArrivalMessage message);

    /**
     * Tell the scheduler a that a drone has insufficient battery to finish his assignment
     * @param message message containing the drone, the current location and remaining battery percentage.
     */
    protected abstract void receiveDroneBatteryMessage(DroneBatteryMessage message);

}
