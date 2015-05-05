package CollisionDetector;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import droneapi.api.DroneCommander;
import droneapi.model.properties.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

/**
 * Created by Sander on 29/04/2015.
 */
public class CollisionDetector extends AbstractActor {

    //Minimum distance between 2 drones.
    private static final double MIN_DISTANCE = 5;
    //Minimum height of a drone to be able to collide
    private static final double MIN_COLLIDE_HEIGHT = 0.5;
    //Minimum height between 2 drones on the same location
    private static final double MIN_HEIGHT = 0.5;

    private Map<Long,Location> drones = new HashMap<>();
    private List<ActorRef> senders = new ArrayList<>();

    private ActorRef reporterRef;
    private boolean collison = false;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public CollisionDetector(List<Long> droneList, ActorRef reporterRef) {
        this.reporterRef = reporterRef;

        for(Long droneId: droneList){
            drones.put(droneId,null);
            senders.add(getContext().system().actorOf(Props.create(LocationChangedMessageSender.class,
                    () -> new LocationChangedMessageSender(droneId, self()))));
        }

        receive(ReceiveBuilder.
                        match(CollisionDetectorStopMessage.class, s -> collisionDetectorStopMessage(s)).
                        match(CollisionLocationChangedMessage.class, s -> collisionLocationChangedMessage(s)).build()
        );

        log.info("CollisionDetector has been started.");
    }


    private void collisionDetectorStopMessage(CollisionDetectorStopMessage s){
        log.info("CollisionDetector has been stopped.");
        for(ActorRef lcms: senders){
            lcms.tell(s,sender());
        }
        reporterRef.tell(collison, self());
        getContext().stop(self());
    }

    private void collisionLocationChangedMessage(CollisionLocationChangedMessage m){
        if(collison){
            return;
        }

        Location droneLocation = m.getLocation();
        drones.put(m.getDroneId(),droneLocation);

        //Check if drone collide with other drone that is flying
        for(Long otherDrone: drones.keySet()){
            if(otherDrone == m.getDroneId()){
                continue;
            }
            Location otherDroneLocation = drones.get(otherDrone);
            if(otherDroneLocation == null || otherDroneLocation.getHeight() < MIN_COLLIDE_HEIGHT || droneLocation.getHeight() < MIN_COLLIDE_HEIGHT){
                continue;
            }

            //Check if collide
            if(otherDroneLocation.distance(droneLocation) < MIN_DISTANCE && abs(otherDroneLocation.getHeight() - droneLocation.getHeight()) < MIN_HEIGHT){
                log.info("CollisionDetector has detected a collision between drone " + m.getDroneId() + " and " + otherDrone + ".\n" +
                        "   at location " + droneLocation + ", " +otherDroneLocation);
                collison = true;
                return;
            }
        }
    }

}
