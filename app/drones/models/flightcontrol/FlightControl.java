package drones.models.flightcontrol;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import akka.event.Logging;
import akka.event.LoggingAdapter;
/**
 * Created by Sander on 16/03/2015.
 *
 * Flight control for the drones.
 */
public abstract class FlightControl extends AbstractActor{

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public FlightControl(){
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
