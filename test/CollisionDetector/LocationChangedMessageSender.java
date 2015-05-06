package CollisionDetector;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import droneapi.api.DroneCommander;
import droneapi.messages.LocationChangedMessage;
import drones.models.Fleet;
import models.Drone;

/**
 * Created by Sander on 30/04/2015.
 */
public class LocationChangedMessageSender extends AbstractActor{

    private DroneCommander dc;
    private Long droneId;
    private ActorRef reporterRef;

    public LocationChangedMessageSender(Long droneId, ActorRef reporterRef) {
        this.droneId = droneId;
        this.reporterRef = reporterRef;

        receive(ReceiveBuilder.
                        match(CollisionDetectorStopMessage.class, s -> collisionDetectorStopMessage()).
                        match(LocationChangedMessage.class, s -> locationChangedMessage(s)).build()
        );

        //get Drone
        Drone drone = Drone.FIND.byId(droneId);
        dc = Fleet.getFleet().getCommanderForDrone(drone);

        dc.subscribeTopic(self(),LocationChangedMessage.class);
    }

    private void collisionDetectorStopMessage(){
        dc.unsubscribe(self());
        getContext().stop(self());
    }

    private void locationChangedMessage(LocationChangedMessage m){
        reporterRef.tell(new CollisionLocationChangedMessage(m,droneId),self());
    }
}
