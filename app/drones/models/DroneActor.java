package drones.models;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.*;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class DroneActor extends AbstractActor {

    protected LazyProperty<FlyingState> state;
    protected LazyProperty<Location> location;
    protected LazyProperty<Byte> batteryPercentage;
    protected LazyProperty<Void> flatTrimStatus;

    private boolean loaded = false;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public DroneActor(){
        receive(createListeners(). //register specific handlers for implementation

                // External -> drone
                match(LocationRequestMessage.class, s -> handleMessage(location.getValue(), sender(), self())).
                match(FlyingStateRequestMessage.class, s -> handleMessage(state.getValue(), sender(), self())).
                match(BatteryPercentageRequestMessage.class, s -> handleMessage(state.getValue(), sender(), self())).
                match(InitRequestMessage.class, s -> initInternal(sender(), self())).
                match(TakeOffRequestMessage.class, s -> takeOffInternal(sender(), self())).
                match(LandRequestMessage.class, s -> landInternal(sender(), self())).

                // Drone -> external
                        match(LocationChangedMessage.class, s -> location.setValue(new Location(s.getLatitude(), s.getLongitude(), s.getGpsHeigth()))).
                match(BatteryPercentageChangedMessage.class, s -> batteryPercentage.setValue(s.getPercent())).
                match(FlyingStateChangedMessage.class, s -> state.setValue(s.getState())).
                match(FlatTrimChangedMessage.class, s -> flatTrimStatus.setValue(null)).
                matchAny(o -> log.info("received unknown message.")).build());
        }

    protected <T> void handleMessage(final Future<T> value, final ActorRef sender, final ActorRef self){
        final ExecutionContext ec = getContext().system().dispatcher();
        value.onSuccess(new OnSuccess<T>() {
            @Override
            public void onSuccess(T result) throws Throwable {
                sender.tell(result, self);
            }
        }, ec);
        value.onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) throws Throwable {
                log.debug("Future failure: [{}]", failure.getMessage());
                sender.tell(new akka.actor.Status.Failure(failure), self);
            }
        }, ec);
    }

    private void initInternal(final ActorRef sender, final ActorRef self){
        if(!loaded){
            loaded = true;
            log.debug("Attempting init.");
            Promise<Void> p = Futures.promise();
            handleMessage(p.future(), sender, self);
            init(p);
        }
    }

    private void takeOffInternal(final ActorRef sender, final ActorRef self){
        if(!loaded || state.getRawValue() != FlyingState.LANDED){
            sender.tell(new akka.actor.Status.Failure(new DroneException("Cannot takeoff when not on ground / not initialized.")), self);
        } else {
            log.debug("Attempting takeoff.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            takeOff(v);
        }
    }

    private void landInternal(final ActorRef sender, final ActorRef self){
        if(loaded){
            log.debug("Attempting landing... (pray to cthullu that this works!)");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            land(v);
        } else {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone not initialized yet")), self);
        }
    }

    protected abstract void init(Promise<Void> p);
    protected abstract void takeOff(Promise<Void> p);
    protected abstract void land(Promise<Void> p);

    protected abstract UnitPFBuilder<Object> createListeners();
}
