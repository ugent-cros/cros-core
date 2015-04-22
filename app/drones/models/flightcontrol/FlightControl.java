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

    protected ActorRef reporterRef;

    protected static final double DEFAULT_ALTITUDE = 2;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public FlightControl(ActorRef reporterRef) {
        this.reporterRef = reporterRef;

        //Receive behaviour
        receive(createListeners().
                        match(StartFlightControlMessage.class, s -> start()).
                        match(RequestForLandingMessage.class, s -> requestForLandingMessage(s)).
                        match(RequestForLandingGrantedMessage.class, s -> requestForLandingGrantedMessage(s)).
                        match(LandingCompletedMessage.class, s -> landingCompletedMessage(s)).
                        match(RequestForTakeOffMessage.class, s -> requestForTakeOffMessage(s)).
                        match(RequestForTakeOffGrantedMessage.class, s -> requestForTakeOffGrantedMessage(s)).
                        match(TakeOffCompletedMessage.class, s -> takeOffCompletedMessage(s)).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    protected abstract UnitPFBuilder<Object> createListeners();

    /**
     * Start flying the drones when all initialization parameters are set.
     */
    public abstract void start();

    protected abstract void requestForLandingMessage(RequestForLandingMessage m);

    protected abstract void requestForLandingGrantedMessage(RequestForLandingGrantedMessage m);

    protected abstract void landingCompletedMessage(LandingCompletedMessage m);

    protected abstract void requestForTakeOffMessage(RequestForTakeOffMessage m);

    protected abstract void requestForTakeOffGrantedMessage(RequestForTakeOffGrantedMessage m);

    protected abstract void takeOffCompletedMessage(TakeOffCompletedMessage m);
}
