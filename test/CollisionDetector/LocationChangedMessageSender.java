package CollisionDetector;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import droneapi.api.DroneCommander;
import droneapi.messages.LocationChangedMessage;

/**
 * Created by Sander on 30/04/2015.
 */
public class LocationChangedMessageSender extends AbstractActor{

    private DroneCommander dc;
    private ActorRef reporterRef;

    public LocationChangedMessageSender(DroneCommander dc, ActorRef reporterRef) {
        this.dc = dc;
        this.reporterRef = reporterRef;

        receive(ReceiveBuilder.
                        match(CollisionDetectorStopMessage.class, s -> collisionDetectorStopMessage()).
                        match(LocationChangedMessage.class, s -> locationChangedMessage(s)).build()
        );

        dc.subscribeTopic(self(),LocationChangedMessage.class);
    }

    private void collisionDetectorStopMessage(){
        dc.unsubscribe(self());
        getContext().stop(self());
    }

    private void locationChangedMessage(LocationChangedMessage m){
        reporterRef.tell(new CollisionLocationChangedMessage(m,dc),self());
    }
}
