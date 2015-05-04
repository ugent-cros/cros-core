package drones.scheduler;

import akka.actor.ActorRef;
import akka.event.japi.LookupEventBus;
import drones.scheduler.messages.from.SchedulerEvent;
import play.Logger;

/**
 * Created by Ronald on 13/04/2015.
 */
public class SchedulerEventBus extends LookupEventBus<SchedulerEvent,ActorRef,Class> {

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
