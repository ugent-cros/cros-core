package drones.models.flightcontrol;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.event.Logging;
import akka.event.LoggingAdapter;
/**
 * Created by Sander on 16/03/2015.
 *
 * Flight control for the drones.
 */
public abstract class FlightControl extends AbstractActor{

    protected ActorRef actorRef;

    protected static final double DEFAULT_ALTITUDE = 5;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public FlightControl(ActorRef actorRef){
        this.actorRef = actorRef;

        //Receive behaviour
        receive(ReceiveBuilder.
                        match(StartFlightControlMessage.class, s -> start()).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    /**
     * Start flying the drones when all initialization parameters are set.
     */
    public abstract void start();
}
