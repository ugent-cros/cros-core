package drones.models;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.*;
import scala.Function1;
import scala.PartialFunction;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.runtime.BoxedUnit;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class DroneActor extends AbstractActor {

    protected LazyProperty<FlyingState> state;
    protected LazyProperty<AlertState> alertState;
    protected LazyProperty<Location> location;
    protected LazyProperty<Byte> batteryPercentage;
    protected LazyProperty<Void> flatTrimStatus;
    protected LazyProperty<Rotation> rotation;
    protected LazyProperty<Speed> speed;
    protected LazyProperty<Double> altitude;
    protected LazyProperty<DroneVersion> version;

    protected DroneEventBus eventBus;

    private boolean loaded = false;
    private boolean loading = false;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public DroneActor() {
        eventBus = new DroneEventBus();

        batteryPercentage = new LazyProperty<>();
        state = new LazyProperty<>(FlyingState.LANDED); //TODO: check assumption of connecting in-flight
        alertState = new LazyProperty<>(AlertState.NONE);
        flatTrimStatus = new LazyProperty<>();
        location = new LazyProperty<>();
        rotation = new LazyProperty<>();
        speed = new LazyProperty<>();
        altitude = new LazyProperty<>();
        version = new LazyProperty<>();

        // TODO: build pipeline that directly forwards to the eventbus

        receive(createListeners(). //register specific handlers for implementation

                // General property requests
                match(PropertyRequestMessage.class, this::handlePropertyRequest).

                // General commands (can be converted to switch as well, depends on embedded data)
                        match(InitRequestMessage.class, s -> initInternal(sender(), self())).
                match(TakeOffRequestMessage.class, s -> takeOffInternal(sender(), self())).
                match(LandRequestMessage.class, s -> landInternal(sender(), self())).
                match(MoveRequestMessage.class, s -> moveInternal(sender(), self(), s)).
                match(SetMaxHeigthRequestMessage.class, s -> setMaxHeightInternal(sender(), self(), s.getMeters())).
                match(SetMaxTiltRequestMessage.class, s -> setMaxTiltInternal(sender(), self(), s.getDegrees())).
                match(SubscribeEventMessage.class, s -> handleSubscribeMessage(sender(), s.getSubscribedClass())).
                match(UnsubscribeEventMessage.class, s -> handleUnsubscribeMessage(sender(), s.getSubscribedClass())).


                // Drone -> external
                        match(LocationChangedMessage.class, s -> {
                    location.setValue(new Location(s.getLatitude(), s.getLongitude(), s.getGpsHeigth()));
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(BatteryPercentageChangedMessage.class, s -> {
                    batteryPercentage.setValue(s.getPercent());
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(FlyingStateChangedMessage.class, s -> {
                    state.setValue(s.getState());
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(AlertStateChangedMessage.class, s -> {
                    alertState.setValue(s.getState());
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(FlatTrimChangedMessage.class, s -> {
                    flatTrimStatus.setValue(null);
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(AttitudeChangedMessage.class, s -> {
                    rotation.setValue(new Rotation(s.getRoll(), s.getPitch(), s.getYaw()));
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(AltitudeChangedMessage.class, s -> {
                    altitude.setValue(s.getAltitude());
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(SpeedChangedMessage.class, s -> {
                    speed.setValue(new Speed(s.getSpeedX(), s.getSpeedY(), s.getSpeedZ()));
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(ProductVersionChangedMessage.class, s -> {
                    version.setValue(new DroneVersion(s.getSoftware(), s.getHardware()));
                    eventBus.publish(new DroneEventMessage(s));
                }).
                matchAny(o -> log.info("DroneActor unk message recv: [{}]", o.getClass().getCanonicalName())).build());
    }

    private void handleSubscribeMessage(final ActorRef sub, Class cl) {
        eventBus.subscribe(sub, cl);
    }

    private void handleUnsubscribeMessage(final ActorRef sub, Class cl) {
        if (cl == null) {
            eventBus.unsubscribe(sub);
        } else {
            eventBus.unsubscribe(sub, cl);
        }
    }

    protected void handlePropertyRequest(PropertyRequestMessage msg) {
        switch (msg.getType()) {
            case LOCATION:
                handleMessage(location.getValue(), sender(), self());
                break;
            case ALTITUDE:
                handleMessage(altitude.getValue(), sender(), self());
                break;
            case BATTERY:
                handleMessage(batteryPercentage.getValue(), sender(), self());
                break;
            case FLATTRIMSTATUS:
                handleMessage(flatTrimStatus.getValue(), sender(), self());
                break;
            case FLYINGSTATE:
                handleMessage(state.getValue(), sender(), self());
                break;
            case ROTATION:
                handleMessage(rotation.getValue(), sender(), self());
                break;
            case SPEED:
                handleMessage(speed.getValue(), sender(), self());
                break;
            case VERSION:
                handleMessage(version.getValue(), sender(), self());
                break;
            default:
                log.warning("No property handler for: [{}]", msg.getType());
                break;
        }
    }

    protected <T> void handleMessage(final Future<T> value, final ActorRef sender, final ActorRef self) {
        final ExecutionContext ec = getContext().system().dispatcher();
        value.onSuccess(new OnSuccess<T>() {
            @Override
            public void onSuccess(T result) throws Throwable {
                sender.tell(new ExecutionResultMessage(result), self); // prevent message is null error
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

    private void moveInternal(final ActorRef sender, final ActorRef self, final MoveRequestMessage msg) {
        if (!loaded || state.getRawValue() == FlyingState.LANDED) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone cannot move when on the ground")), self);
        } else {
            log.debug("Attempting movement vx=[{}], vy=[{}], vz=[{}], vr=[{}]", msg.getVx(), msg.getVy(), msg.getVz(), msg.getVr());
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            move3d(v, msg.getVx(), msg.getVy(), msg.getVz(), msg.getVr());
        }
    }

    private void initInternal(final ActorRef sender, final ActorRef self) {
        if (!loading && !loaded) {
            loading = true;
            final ExecutionContext ec = getContext().system().dispatcher();

            log.debug("Attempting init.");

            Promise<Void> p = Futures.promise();
            p.future().onFailure(new OnFailure() {
                @Override
                public void onFailure(Throwable failure) throws Throwable {
                    loading = false;
                    loaded = false;
                }
            }, ec);
            p.future().onSuccess(new OnSuccess<Void>() {
                @Override
                public void onSuccess(Void result) throws Throwable {
                    loaded = true;
                    loading = false;
                }
            }, ec);
            handleMessage(p.future(), sender, self);
            init(p);
        }
    }

    private void takeOffInternal(final ActorRef sender, final ActorRef self) {
        if (!loaded || state.getRawValue() != FlyingState.LANDED) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Cannot takeoff when not on ground / not initialized.")), self);
        } else {
            log.debug("Attempting takeoff.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            takeOff(v);
        }
    }

    private void landInternal(final ActorRef sender, final ActorRef self) {
        if (loaded) {
            log.debug("Attempting landing... (pray to cthullu that this works!)");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            land(v);
        } else {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone not initialized yet")), self);
        }
    }

    private void setMaxHeightInternal(final ActorRef sender, final ActorRef self, float meters) {
        if (loaded) {
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            setMaxHeight(v, meters);
        } else {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone not initialized yet")), self);
        }
    }

    private void setMaxTiltInternal(final ActorRef sender, final ActorRef self, float degrees) {
        if (loaded) {
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            setMaxTilt(v, degrees);
        } else {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone not initialized yet")), self);
        }
    }

    protected abstract void init(Promise<Void> p);

    protected abstract void takeOff(Promise<Void> p);

    protected abstract void land(Promise<Void> p);

    protected abstract void move3d(Promise<Void> p, double vx, double vy, double vz, double vr);

    protected abstract void setMaxHeight(Promise<Void> p, float meters);

    protected abstract void setMaxTilt(Promise<Void> p, float degrees);

    protected abstract UnitPFBuilder<Object> createListeners();
}
