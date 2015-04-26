package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.event.japi.LookupEventBus;
import drones.models.scheduler.messages.from.SchedulerEvent;
import play.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Created by Ronald on 13/04/2015.
 */
public class SchedulerEventBus extends LookupEventBus<SchedulerEvent,ActorRef,Class> {

    @Override
    public int mapSize() {
        return 32;
    }

    @Override
    public void publish(SchedulerEvent event, ActorRef subscriber) {
        Logger.debug("PUBLISHING: " + event.getClass().getSimpleName());
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
