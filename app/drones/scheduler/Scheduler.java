package drones.scheduler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.scheduler.messages.from.SchedulerEvent;
import drones.scheduler.messages.from.SchedulerReplyMessage;
import drones.scheduler.messages.from.SubscribedMessage;
import drones.scheduler.messages.from.UnsubscribedMessage;
import drones.scheduler.messages.to.*;
import models.Assignment;
import models.Drone;
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
     *
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
     *
     * @param type the type of scheduler to be used, has to be a subclass of Scheduler
     * @throws SchedulerException
     */
    public static void start(Class<? extends Scheduler> type) throws SchedulerException {
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
     *
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
     *
     * @param type       type of events to subscribe to
     * @param subscriber actorref to receive events
     * @throws SchedulerException
     */
    public static void subscribe(Class<? extends SchedulerEvent> type, ActorRef subscriber) throws SchedulerException {
        ActorRef publisher = getScheduler();
        SubscribeMessage message = new SubscribeMessage(type);
        publisher.tell(message, subscriber);
    }

    /**
     * Unsubscribe from the scheduler for a specific type of events.
     *
     * @param type
     * @param subscriber
     * @throws SchedulerException
     */
    public static void unsubscribe(Class<? extends SchedulerEvent> type, ActorRef subscriber) throws SchedulerException {
        ActorRef publisher = getScheduler();
        UnsubscribeMessage message = new UnsubscribeMessage(type);
        publisher.tell(message, subscriber);
    }

    /**
     * Force the scheduler to start scheduling
     *
     * @throws SchedulerException
     */
    public static void schedule() throws SchedulerException {
        getScheduler().tell(new ScheduleMessage(), ActorRef.noSender());
    }

    /**
     * Cancel an assignment safely.
     *
     * @param assignmentId id of the assignment to cancel.
     * @throws SchedulerException
     */
    public static void cancelAssignment(long assignmentId) throws SchedulerException {
        getScheduler().tell(new CancelAssignmentMessage(assignmentId), ActorRef.noSender());
    }

    /**
     * Provide a new drone to the scheduler to assign assignments.
     *
     * @param droneId id of the drone to add to the pool
     * @throws SchedulerException
     */
    public static void addDrone(long droneId) throws SchedulerException {
        getScheduler().tell(new AddDroneMessage(droneId), ActorRef.noSender());
    }

    /**
     * Tell the scheduler to try adding all the drones from the database.
     *
     * @throws SchedulerException
     */
    public static void addDrones() throws SchedulerException {
        ActorRef scheduler = getScheduler();
        List<Drone> drones = Drone.FIND.all();
        for (Drone drone : drones) {
            if (drone.getStatus() == Drone.Status.AVAILABLE) {
                scheduler.tell(new AddDroneMessage(drone.getId()), ActorRef.noSender());
            }
        }
    }

    /**
     * Prohibit the scheduler from using a particular drone for assignments.
     *
     * @param droneId id of the drone to be removed from the active pool
     * @throws SchedulerException
     */
    public static void removeDrone(long droneId) throws SchedulerException {
        getScheduler().tell(new RemoveDroneMessage(droneId), ActorRef.noSender());
    }

    /**
     * Tell the scheduler to land a drone immediately.
     *
     * @param droneId
     * @throws SchedulerException
     */
    public static void emergency(long droneId) throws SchedulerException {
        getScheduler().tell(new EmergencyMessage(droneId), ActorRef.noSender());
    }

    /**
     * Force the scheduler to publish an event.
     *
     * @param event
     * @throws SchedulerException
     */
    public static void publishEvent(SchedulerEvent event) throws SchedulerException {
        getScheduler().tell(new SchedulerPublishMessage(event), ActorRef.noSender());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    protected SchedulerEventBus eventBus;
    private static ActorRef scheduler;
    private static Object lock = new Object();

    protected Scheduler() {
        // Create an event bus for listeners
        eventBus = new SchedulerEventBus();
        //Receive behaviour
        UnitPFBuilder<Object> builder = initReceivers();
        builder.matchAny(m -> log.warning("[Scheduler] Received unknown message: [{}]", m.getClass().getName()));
        receive(builder.build());
    }

    protected UnitPFBuilder<Object> initReceivers() {
        return ReceiveBuilder
                .match(SubscribeMessage.class, m -> subscribe(m))
                .match(UnsubscribeMessage.class, m -> unsubscribe(m))
                .match(SchedulerRequestMessage.class, m -> reply(m))
                .match(EmergencyMessage.class, m -> emergency(m))
                .match(ScheduleMessage.class, m -> schedule(m))
                .match(StopSchedulerMessage.class, m -> stop(m))
                .match(CancelAssignmentMessage.class, m -> cancelAssignment(m))
                .match(AddDroneMessage.class, m -> addDrone(m))
                .match(RemoveDroneMessage.class, m -> removeDrone(m))
                .match(SchedulerPublishMessage.class, m -> eventBus.publish(m.getEvent()));
    }

    private void subscribe(SubscribeMessage message) {
        eventBus.subscribe(sender(), message.getEventType());
        sender().tell(new SubscribedMessage(message.getEventType()), self());
    }

    private void unsubscribe(UnsubscribeMessage message) {
        eventBus.unsubscribe(sender(), message.getEventType());
        sender().tell(new UnsubscribedMessage(message.getEventType()), self());
    }

    private void reply(SchedulerRequestMessage message) {
        eventBus.publish(new SchedulerReplyMessage(message.getRequestId()));
    }

    protected Drone getDrone(long droneId) {
        return Drone.FIND.byId(droneId);
    }

    protected Assignment getAssignment(long assignmentId) {
        return Assignment.FIND.byId(assignmentId);
    }

    /**
     * Handle a drone emergency message
     *
     * @param message
     */
    protected abstract void emergency(EmergencyMessage message);

    /**
     * Force the scheduler to execute schedule procedure.
     *
     * @param message containing sequenceId
     */
    protected abstract void schedule(ScheduleMessage message);

    /**
     * Tell the scheduler to stop all flights and terminate itself.
     *
     * @param message
     */
    protected abstract void stop(StopSchedulerMessage message);

    /**
     * Tell the scheduler to cancel an assignment, regardless whether it is assigned to a drone.
     *
     * @param message
     */
    protected abstract void cancelAssignment(CancelAssignmentMessage message);

    /**
     * Tell the scheduler to add a drone to the drone pool.
     *
     * @param message
     */
    protected abstract void addDrone(AddDroneMessage message);

    /**
     * Tell the scheduler to remove a drone from his drone pool, regardless whether it is executing an assignment.
     *
     * @param message
     */
    protected abstract void removeDrone(RemoveDroneMessage message);
}
