package drones.models.scheduler;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.models.scheduler.messages.from.SchedulerEvent;
import drones.models.scheduler.messages.from.SchedulerReplyMessage;
import drones.models.scheduler.messages.to.*;
import models.Assignment;
import models.Drone;
import models.Location;
import play.libs.Akka;

import java.util.List;

/**
 * Created by Ronald on 16/03/2015.
 */

/*
Class to schedule assignments.
 */
public abstract class Scheduler extends AbstractActor {


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // STATIC METHODS TO COMMUNICATE WITH THE SCHEDULER EASILY
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Receive an actor reference for the scheduler.
     * At one time, there can only be one scheduler.
     * @return an ActorRef for the scheduler.
     * @throws SchedulerException
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

    /**
     * Start the scheduler.
     * This will create an new actor for the scheduler if there isn't one yet.
     * @param type  the type of scheduler to be used, has to be a subclass of Scheduler
     * @throws SchedulerException
     */
    public static void start(Class<? extends Scheduler> type) throws SchedulerException{
        synchronized (lock) {
            if (scheduler == null || scheduler.isTerminated()) {
                scheduler = Akka.system().actorOf(Props.create(type));
            } else {
                throw new SchedulerException("The scheduler has already been started.");
            }
        }
    }

    /**
     * Stop the scheduler.
     * This will tell the scheduler to cancel all flights safely.
     * @throws SchedulerException
     */
    public static void stop() throws SchedulerException {
        synchronized (lock) {
            if (scheduler != null) {
                if (!scheduler.isTerminated()) {
                    scheduler.tell(new StopSchedulerMessage(), ActorRef.noSender());
                }
                scheduler = null;
            } else {
                throw new SchedulerException("The scheduler cannot be stopped before it has started.");
            }
        }
    }

    /**
     * Subscribe to get notified by scheduler events.
     * @param type   type of events to subscribe to
     * @param subscriber    actorref to receive events
     * @throws SchedulerException
     */
    public static void subscribe(Class<? extends SchedulerEvent> type,ActorRef subscriber) throws SchedulerException{
        ActorRef publisher = getScheduler();
        SubscribeMessage message = new SubscribeMessage(type);
        publisher.tell(message,subscriber);
    }

    /**
     * Unsubscribe from the scheduler for a specific type of events.
     * @param type
     * @param subscriber
     * @throws SchedulerException
     */
    public static void unsubscribe(Class<? extends SchedulerEvent> type,ActorRef subscriber) throws SchedulerException{
        ActorRef publisher = getScheduler();
        UnsubscribeMessage message = new UnsubscribeMessage(type);
        publisher.tell(message,subscriber);
    }

    /**
     * Force the scheduler to start scheduling
     * @throws SchedulerException
     */
    public static void schedule() throws SchedulerException{
        getScheduler().tell(new ScheduleMessage(), ActorRef.noSender());
    }

    /**
     * Cancel an assignment safely.
     * @param assignmentId id of the assignment to cancel.
     * @throws SchedulerException
     */
    public static void cancelAssignment(long assignmentId) throws SchedulerException{
        getScheduler().tell(new CancelAssignmentMessage(assignmentId), ActorRef.noSender());
    }

    /**
     * Provide a new drone to the scheduler to assign assignments.
     * @param droneId id of the drone to add to the pool
     * @throws SchedulerException
     */
    public static void addDrone(long droneId) throws SchedulerException{
        getScheduler().tell(new AddDroneMessage(droneId),ActorRef.noSender());
    }

    /**
     * Tell the scheduler to try adding all the drones from the database.
     * @throws SchedulerException
     */
    public static void addDrones() throws SchedulerException{
        ActorRef scheduler = getScheduler();
        List<Drone> drones = Drone.FIND.all();
        for (Drone drone : drones){
            scheduler.tell(new AddDroneMessage(drone.getId()),ActorRef.noSender());
        }
    }

    /**
     * Prohibit the scheduler from using a particular drone for assignments.
     * @param droneId id of the drone to be removed from the active pool
     * @throws SchedulerException
     */
    public static void removeDrone(long droneId) throws SchedulerException{
        getScheduler().tell(new RemoveDroneMessage(droneId), ActorRef.noSender());
    }

    /**
     * Force the scheduler to publish an event.
     * @param event
     * @throws SchedulerException
     */
    public static void publishEvent(SchedulerEvent event) throws SchedulerException{
        getScheduler().tell(new PublishMessage(event), ActorRef.noSender());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    protected SchedulerEventBus eventBus;
    private static ActorRef scheduler;
    private static Object lock = new Object();

    protected Scheduler() {
        // Create an eventbus for listeners
        eventBus = new SchedulerEventBus();
        //Receive behaviour
        UnitPFBuilder<Object> builder = initReceivers();
        builder.matchAny(m -> log.warning("[Scheduler] Received unknown message: [{}]", m.getClass().getName()));
        receive(builder.build());
    }

    protected UnitPFBuilder<Object> initReceivers() {
        return ReceiveBuilder
                .match(SchedulerRequestMessage.class, m -> reply(m.getRequestId()))
                .match(ScheduleMessage.class, m -> schedule(m))
                .match(DroneArrivalMessage.class, m -> droneArrived(m.getDroneId()))
                .match(DroneBatteryMessage.class, m -> receiveDroneBatteryMessage(m))
                .match(SubscribeMessage.class, m -> eventBus.subscribe(sender(), m.getEventType()))
                .match(UnsubscribeMessage.class, m -> eventBus.unsubscribe(sender(), m.getEventType()))
                .match(StopSchedulerMessage.class, m -> stop(m));
    }

    /**
     * Reply to a request message by publishing a reply message with the same request id.
     * @param requestId
     */
    private void reply(long requestId){
        eventBus.publish(new SchedulerReplyMessage(requestId));
    }

    /**
     * Updates the dispatch in the database.
     * @param drone      dispatched drone
     * @param assignment assigned assignment
     */
    protected void assign(Drone drone, Assignment assignment) {
        // Update drone
        drone.setStatus(Drone.Status.FLYING);
        drone.update();
        // Update assignment
        assignment.setAssignedDrone(drone);
        assignment.update();
    }

    /**
     * Updates the arrival of a drone in the database
     * @param drone      drone that arrived
     * @param assignment assignment that has been completed by arrival
     */
    protected void unassign(Drone drone, Assignment assignment) {
        // Update drone
        if (drone.getStatus() == Drone.Status.FLYING) {
            // Set state available again if possible
            drone.setStatus(Drone.Status.AVAILABLE);
            drone.update();
        }
        // Update assignment
        assignment.setAssignedDrone(null);
        assignment.update();
    }

    /**
     * Force the scheduler to execute schedule procedure.
     * @param message containing sequenceId
     */
    protected abstract void schedule(ScheduleMessage message);

    /**
     * Tell the scheduler a drone has arrived at it's destination.
     * @param droneId id of the drone that arrived
     */
    protected abstract void droneArrived(long droneId);

    /**
     * Tell the scheduler a that a drone has insufficient battery to finish his assignment
     * @param message message containing the drone, the current location and remaining battery percentage.
     */
    protected abstract void receiveDroneBatteryMessage(DroneBatteryMessage message);

    /**
     * Tell the scheduler to stop all flights and terminate itself.
     * @param message
     */
    protected abstract void stop(StopSchedulerMessage message);
}
