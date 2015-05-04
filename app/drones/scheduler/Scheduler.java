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
    public static ActorRef getScheduler(){
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
    public static void start(Class<? extends Scheduler> type){
        synchronized (lock) {
            if (scheduler == null || scheduler.isTerminated()) {
                scheduler = Akka.system().actorOf(Props.create(type),"CROS-Scheduler");
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
    public static void stop(){
        synchronized (lock) {
            if (scheduler == null || scheduler.isTerminated()) {
                throw new SchedulerException("The scheduler is already terminated.");
            } else {
                scheduler.tell(new StopSchedulerMessage(), ActorRef.noSender());
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
    public static void subscribe(Class<? extends SchedulerEvent> type, ActorRef subscriber){
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
    public static void unsubscribe(Class<? extends SchedulerEvent> type, ActorRef subscriber){
        ActorRef publisher = getScheduler();
        UnsubscribeMessage message = new UnsubscribeMessage(type);
        publisher.tell(message, subscriber);
    }

    /**
     * Try to schedule an assignment.
     * @param assignmentId
     */
    public static void scheduleAssignment(long assignmentId){
        getScheduler().tell(new ScheduleAssignmentMessage(assignmentId), ActorRef.noSender());
    }

    /**
     * Try to schedule a drone.
     * @param droneId
     */
    public static void scheduleDrone(long droneId){
        getScheduler().tell(new ScheduleDroneMessage(droneId), ActorRef.noSender());
    }


    /**
     * Cancel an assignment safely.
     *
     * @param assignmentId id of the assignment to cancel.
     * @throws SchedulerException
     */
    public static void cancelAssignment(long assignmentId){
        getScheduler().tell(new CancelAssignmentMessage(assignmentId), ActorRef.noSender());
    }

    /**
     * Tell the scheduler to land a drone immediately.
     *
     * @param droneId
     * @throws SchedulerException
     */
    public static void setDroneEmergency(long droneId){
        getScheduler().tell(new DroneEmergencyMessage(droneId), ActorRef.noSender());
    }

    /**
     * Force the scheduler to publish an event.
     *
     * @param event
     * @throws SchedulerException
     */
    public static void publishEvent(SchedulerEvent event){
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
                .match(StartSchedulerMessage.class, m -> start(m))
                .match(StopSchedulerMessage.class, m -> stop(m))
                .match(ScheduleAssignmentMessage.class, m -> scheduleAssignment(m))
                .match(ScheduleDroneMessage.class, m -> scheduleDrone(m))
                .match(CancelAssignmentMessage.class, m -> cancelAssignment(m))
                .match(DroneEmergencyMessage.class, m -> droneEmergency(m))
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
    protected abstract void scheduleAssignment(ScheduleAssignmentMessage message);
    protected abstract void scheduleDrone(ScheduleDroneMessage message);
    protected abstract void start(StartSchedulerMessage message);
    protected abstract void stop(StopSchedulerMessage message);
    protected abstract void cancelAssignment(CancelAssignmentMessage message);
    protected abstract void droneEmergency(DroneEmergencyMessage message);
}
