package drones.models;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import droneapi.messages.SubscribeEventMessage;
import droneapi.messages.UnsubscribeEventMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Cedric on 4/16/2015.
 */
public class BroadcastBus extends UntypedActor {

    private final ConcurrentLinkedQueue<ActorRef> subscribers;

    public BroadcastBus(){
        subscribers = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof SubscribeEventMessage) {
            subscribers.add(sender());
        } else if(message instanceof UnsubscribeEventMessage){
            subscribers.remove(sender());
        } else {
            for(ActorRef sub : subscribers) {
                sub.forward(message, getContext());
            }
        }
    }
}
