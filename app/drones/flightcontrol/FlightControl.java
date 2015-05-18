package drones.flightcontrol;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.UnitPFBuilder;
import drones.flightcontrol.messages.*;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Abstract main class of the flightControl package.
 *
 * Created by Sander on 16/03/2015.
 */
public abstract class FlightControl extends AbstractActor {

    protected static final double DEFAULT_ALTITUDE = 2;

    //max delay times
    protected static final FiniteDuration MAX_DURATION_SHORT = Duration.create(30, TimeUnit.SECONDS);
    protected static final FiniteDuration MAX_DURATION_LONG = Duration.create(120, TimeUnit.SECONDS);

    //actor to report the outgoing messages
    protected ActorRef reporterRef;

    //Boolean to indicate if the flightcontrol is blocked because of an error or because it has not yet started
    protected boolean blocked = true;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     *
     * @param reporterRef actor to report the outgoing messages
     */
    public FlightControl(ActorRef reporterRef) {
        this.reporterRef = reporterRef;

        //Receive behaviour
        receive(createListeners().
                        match(StartFlightControlMessage.class, s -> startFlightControlMessage()).
                        match(StopFlightControlMessage.class, s -> stopFlightControlMessage(s)).
                        match(RequestMessage.class, s -> requestMessage(s)).
                        match(CompletedMessage.class, s -> completedMessage(s)).
                        match(RequestGrantedMessage.class, s -> requestGrantedMessage(s)).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    /**
     * Return a UnitPFBuilder with all needed listeners registered.
     */
    protected abstract UnitPFBuilder<Object> createListeners();

    /**
     * Start flying the drones when all initialization parameters are set.
     */
    protected abstract void startFlightControlMessage();

    /**
     * Handles a StopFlightControlMessage.
     */
    protected abstract void stopFlightControlMessage(StopFlightControlMessage m);

    /**
     * Handles a RequestMessage of another drone. A RequestMessage is sent when a drone wants to land or to take off.
     */
    protected abstract void requestMessage(RequestMessage m);

    /**
     * Handles a RequestGrantedMessage. A RequestGrantedMessage is sent to a class as a reply on a RequestMessage.
     */
    protected abstract void requestGrantedMessage(RequestGrantedMessage m);

    /**
     * Handles CompletedMessage of a other drone. A CompletedMessage is sent when a other drone has completed his landing of take off that he has requested.
     */
    protected abstract void completedMessage(CompletedMessage m);
}
