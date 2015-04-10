package drones.models.flightcontrol;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.UnitPFBuilder;
import drones.models.flightcontrol.messages.*;

/**
 * Created by Sander on 16/03/2015.
 * <p>
 * Flight control for the drones.
 */
public abstract class FlightControl extends AbstractActor {

    protected ActorRef actorRef;

    protected static final double DEFAULT_ALTITUDE = 5;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public FlightControl(ActorRef actorRef) {
        this.actorRef = actorRef;

        //Receive behaviour
        receive(createListeners().
                        match(StartFlightControlMessage.class, s -> start()).
                        match(RequestMessage.class, s -> requestMessage(s)).
                        match(RequestGrantedMessage.class, s -> requestGrantedMessage(s)).
                        match(CompletedMessage.class, s -> completedMessage(s)).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    protected abstract UnitPFBuilder<Object> createListeners();

    /**
     * Start flying the drones when all initialization parameters are set.
     */
    public abstract void start();

    protected abstract void requestMessage(RequestMessage m);

    protected abstract void requestGrantedMessage(RequestGrantedMessage m);

    protected abstract void completedMessage(CompletedMessage m);
}
