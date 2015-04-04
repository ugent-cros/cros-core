package drones.models;

import akka.actor.ActorRef;
import akka.event.japi.LookupEventBus;

/**
 * Created by Cedric on 3/17/2015.
 */
public class DroneEventBus extends LookupEventBus<DroneEventMessage, ActorRef, Class> {

    @Override
    public int mapSize() {
        return 128; //guesstimate of expected num of identifiers (map size)
    }

    @Override
    public int compareSubscribers(ActorRef a, ActorRef b) {
        return a.compareTo(b);
    }

    @Override
    public Class classify(DroneEventMessage event) {
        return event.getIdentifier();
    }

    @Override
    public void publish(DroneEventMessage event, ActorRef subscriber) {
        if(event.getInnerMsg() != null){
            subscriber.tell(event.getInnerMsg(), ActorRef.noSender());
        }
    }
}
