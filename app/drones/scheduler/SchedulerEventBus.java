package drones.scheduler;

import akka.actor.ActorRef;
import akka.event.japi.LookupEventBus;
import droneapi.api.DroneStatus;
import drones.scheduler.messages.from.*;
import play.Logger;

/**
 * Created by Ronald on 13/04/2015.
 */
public class SchedulerEventBus extends LookupEventBus<SchedulerEvent,ActorRef,Class> {

    public static final Class[] EVENTS = {
            AssignmentCanceledMessage.class,
            AssignmentCompletedMessage.class,
            AssignmentProgressedMessage.class,
            AssignmentStartedMessage.class,
            AssignmentStatusMessage.class,
            DroneAssignedMessage.class,
            DroneFailedMessage.class,
            DroneStatusMessage.class,
            DroneUnassignedMessage.class,
            SchedulerStoppedMessage.class
    };

    @Override
    public int mapSize() {
        return 32;
    }

    @Override
    public void publish(SchedulerEvent event) {
        super.publish(event);
        Logger.info(event.toString());
    }

    @Override
    public boolean subscribe(ActorRef subscriber, Class to) {
        if(to == null){
            boolean result = true;
            for(Class event : EVENTS){
                result &= super.subscribe(subscriber,event);
            }
            return result;
        }else{
            return super.subscribe(subscriber, to);
        }
    }

    @Override
    public boolean unsubscribe(ActorRef subscriber, Class from) {
        if(from == null){
            super.unsubscribe(subscriber);
            return true;
        }else{
            return super.unsubscribe(subscriber, from);
        }
    }

    @Override
    public void publish(SchedulerEvent event, ActorRef subscriber) {
        subscriber.tell(event, ActorRef.noSender());
    }

    @Override
    public Class classify(SchedulerEvent event) {
        return event.getClass();
    }

    @Override
    public int compareSubscribers(ActorRef a, ActorRef b) {
        return a.compareTo(b);
    }
}
