package drones.models;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.MoveCommand;
import drones.messages.*;
import drones.util.LocationNavigator;
import org.springframework.context.annotation.Lazy;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

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
    protected LazyProperty<NavigationState> navigationState;
    protected LazyProperty<NavigationStateReason> navigationStateReason;
    protected LazyProperty<Boolean> gpsFix;
    protected LazyProperty<Boolean> isOnline;
    protected LazyProperty<Boolean> calibrationRequired;

    protected DroneEventBus eventBus;

    // Navigation
    private LocationNavigator navigator;
    private final Object navigationLock;

    private boolean loaded = false;
    private boolean loading = false;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public DroneActor() {
        eventBus = new DroneEventBus();
        navigationLock = new Object();

        batteryPercentage = new LazyProperty<>();
        state = new LazyProperty<>(FlyingState.LANDED); //TODO: check assumption of connecting in-flight
        alertState = new LazyProperty<>(AlertState.NONE);
        flatTrimStatus = new LazyProperty<>();
        location = new LazyProperty<>();
        rotation = new LazyProperty<>();
        speed = new LazyProperty<>();
        altitude = new LazyProperty<>();
        version = new LazyProperty<>();
        navigationState = new LazyProperty<>(NavigationState.UNAVAILABLE);
        navigationStateReason = new LazyProperty<>(NavigationStateReason.CONNECTION_LOST);
        gpsFix = new LazyProperty<>(false);
        isOnline = new LazyProperty<>(false);
        calibrationRequired = new LazyProperty<>(false);

        navigator = createNavigator(null, null);

        // TODO: build pipeline that directly forwards to the eventbus
        //TODO: revert quickfix and support null
        UnitPFBuilder<Object> extraListeners = createListeners();
        if (extraListeners == null) { // When null, create a new listener chain
            extraListeners = ReceiveBuilder.match(PropertyRequestMessage.class, this::handlePropertyRequest);
        } else {
            extraListeners = extraListeners.match(PropertyRequestMessage.class, this::handlePropertyRequest);
        }
        receive(extraListeners.
                // General commands (can be converted to switch as well, depends on embedded data)
                        match(InitRequestMessage.class, s -> initInternal(sender(), self())).
                match(TakeOffRequestMessage.class, s -> takeOffInternal(sender(), self())).
                match(FlatTrimRequestMessage.class, s -> flatTrimInternal(sender(), self())).
                match(CalibrateRequestMessage.class, s -> calibrateInternal(sender(), self(), s.hasHull(), s.isOutdoor())).
                match(SetHullRequestMessage.class, s -> setHullInternal(sender(), self(), s.hasHull())).
                match(SetOutdoorRequestMessage.class, s -> setOutdoorInternal(sender(), self(), s.isOutdoor())).
                match(LandRequestMessage.class, s -> landInternal(sender(), self())).
                match(MoveRequestMessage.class, s -> moveInternal(sender(), self(), s)).
                match(SetMaxHeightRequestMessage.class, s -> setMaxHeightInternal(sender(), self(), s.getMeters())).
                match(SetMaxTiltRequestMessage.class, s -> setMaxTiltInternal(sender(), self(), s.getDegrees())).
                match(MoveToLocationRequestMessage.class, s -> moveToLocationInternal(sender(), self(), s)).
                match(MoveToLocationCancellationMessage.class, s -> cancelMoveToLocationInternal(sender(), self())).
                match(SubscribeEventMessage.class, s -> handleSubscribeMessage(sender(), s.getSubscribedClass())).
                match(UnsubscribeEventMessage.class, s -> handleUnsubscribeMessage(sender(), s.getSubscribedClass())).

                // Drone -> external
                match(LocationChangedMessage.class, s -> {
                    Location l = new Location(s.getLatitude(), s.getLongitude(), s.getGpsHeight());
                    location.setValue(l);
                    processLocation(l);
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(GPSFixChangedMessage.class, s -> {
                    log.info("GPS fix changed: [{}]", s.isFixed());
                    gpsFix.setValue(s.isFixed());
                    if(s.isFixed()) {
                        //TODO: publish navigationstatechanged/reason
                        navigationState.setValue(NavigationState.AVAILABLE);
                    }else {
                        navigationState.setValue(NavigationState.UNAVAILABLE);
                    }
                    eventBus.publish(new DroneEventMessage(s));
                }).
                match(BatteryPercentageChangedMessage.class, s -> {
                    batteryPercentage.setValue(s.getPercent());
                    log.info("Battery: {}%", s.getPercent());
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
                    Rotation rot = new Rotation(s.getRoll(), s.getPitch(), s.getYaw());
                    rotation.setValue(rot);
                    //processOrientation(rot);
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
                match(NavigationStateChangedMessage.class, s -> {
                    //navigationState.setValue(s.getState());
                    //navigationStateReason.setValue(s.getReason());
                    //eventBus.publish(new DroneEventMessage(s));
                }).
                match(MagnetoCalibrationStateChangedMessage.class, s -> {
                    calibrationRequired.setValue(s.isCalibrationRequired());
                    eventBus.publish(new DroneEventMessage(s));
                    if(s.isCalibrationRequired())
                        log.warning("Drone requires calibration!!!");
                    else
                        log.info("No drone calibration required.");
                }).
                match(ConnectionStatusChangedMessage.class, s -> {
                    if (!s.isConnected()) {
                        log.warning("Drone network became unreachable.");
                    } else {
                        log.info("Drone network became reachable.");
                    }
                    isOnline.setValue(s.isConnected());
                    eventBus.publish(new DroneEventMessage(s));
                }).
                matchAny(o -> log.info("DroneActor unk message recv: [{}]", o.getClass().getCanonicalName())).build());
    }

    private void processLocation(Location location) {
        synchronized (navigationLock){
            if(navigationState.getRawValue() != NavigationState.IN_PROGRESS)
                return;

            // When there's no gps fix, continue
            if (!gpsFix.getRawValue()) {
                // Stop navigator
                //navigator.setNavigationState(NavigationState.UNAVAILABLE);
                navigator.setCurrentLocation(null);
                navigator.setGoal(null);

                navigationState.setValue(NavigationState.UNAVAILABLE);
                navigationStateReason.setValue(NavigationStateReason.CONNECTION_LOST);
                eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.UNAVAILABLE, NavigationStateReason.CONNECTION_LOST)));
                return;
            }

            // Prefer altitude of non-gps sensor
            if (altitude.getRawValue() > 0) {
                location = new Location(location.getLatitude(), location.getLongitude(), altitude.getRawValue());
            }

            MoveCommand cmd = navigator.update(location);
            if(cmd == null){ // arrived
                log.info("Navigator finished at location [{}] for goal [{}]", location, navigator.getGoal());
                navigationState.setValue(NavigationState.AVAILABLE);
                navigationStateReason.setValue(NavigationStateReason.FINISHED);
                eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.AVAILABLE, NavigationStateReason.FINISHED)));

                navigator.setCurrentLocation(null);
                navigator.setGoal(null);
            } else { // execute the movement command
                Promise<Void> v = Futures.promise();
                v.future().onFailure(new OnFailure() {
                    @Override
                    public void onFailure(Throwable failure) throws Throwable {
                        log.error(failure, "Failed to issue move command for auto navigation.");
                    }
                }, getContext().dispatcher());
                move3d(v, cmd.getVx(), cmd.getVy(), cmd.getVz(), cmd.getVr());
            }
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.create("1 minute"),
                t -> {
                    log.error(t, "DroneActor failure caught by supervisor.");
                    System.err.println(t.getMessage());
                    return SupervisorStrategy.resume(); // Continue on all exceptions!
                });
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
            case NAVIGATIONSTATE:
                handleMessage(navigationState.getValue(), sender(), self());
                break;
            case NAVIGATIONREASON:
                handleMessage(navigationStateReason.getValue(), sender(), self());
                break;
            case GPSFIX:
                handleMessage(gpsFix.getValue(), sender(), self());
                break;
            case NETWORK_STATUS:
                handleMessage(isOnline.getValue(), sender(), self());
                break;
            case CALIBRATION_REQUIRED:
                handleMessage(calibrationRequired.getValue(), sender(), self());
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

    private void setOutdoorInternal(final ActorRef sender, final ActorRef self, boolean outdoor) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone status cannot be changed when not initialized")), self);
        } else {
            log.info("Setting outdoor property.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            setOutdoor(v, outdoor);
        }
    }

    private void setHullInternal(final ActorRef sender, final ActorRef self, boolean hull) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone status cannot be changed when not initialized")), self);
        } else {
            log.info("Setting hull property.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            setHull(v, hull);
        }
    }

    private void flatTrimInternal(final ActorRef sender, final ActorRef self) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone flattrim cannot be changed when not initialized")), self);
        } else {
            log.info("Flat trim requested.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            flatTrim(v);
        }
    }

    private void calibrateInternal(final ActorRef sender, final ActorRef self, boolean hull, boolean outdoor) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone calibration not available when not initialized.")), self);
        } else {
            log.info("Calibration requested.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);

            Promise<Void> outdoorPromise = Futures.promise();

            // Chain promises of outdoor -> hull -> flatTrim
            outdoorPromise.future().onSuccess(new OnSuccess<Void>() {
                @Override
                public void onSuccess(Void result) throws Throwable {
                    Promise<Void> hullPromise = Futures.promise();
                    hullPromise.future().onSuccess(new OnSuccess<Void>() {
                        @Override
                        public void onSuccess(Void result) throws Throwable {
                            flatTrim(v);
                        }
                    }, getContext().system().dispatcher());
                    setHull(hullPromise, hull);
                }
            }, getContext().system().dispatcher());
            setOutdoor(outdoorPromise, outdoor);
        }
    }

    private void cancelMoveToLocationInternal(final ActorRef sender, final ActorRef self) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone cannot move when not initialized")), self);
        } else {
            log.info("Cancelling move to location.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);

            synchronized (navigationLock){
                navigationState.setValue(NavigationState.AVAILABLE);
                navigationStateReason.setValue(NavigationStateReason.FINISHED);
                eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.AVAILABLE, NavigationStateReason.FINISHED)));

                navigator.setGoal(null);
                navigator.setCurrentLocation(null);

                v.success(null);
            }

            // Old code:
            //cancelMoveToLocation(v);
        }
    }

    private void moveToLocationInternal(final ActorRef sender, final ActorRef self, final MoveToLocationRequestMessage msg) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone cannot move when not initialized")), self);
        } else {
            log.info("Navigating to lat=[{}], long=[{}], alt=[{}]", msg.getLatitude(), msg.getLongitude(), msg.getAltitude());
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);

            synchronized(navigationLock){
                if(navigationState.getRawValue() == NavigationState.IN_PROGRESS) {
                    v.failure(new DroneException("Already navigating to " + navigator.getGoal() + ", abort this first."));
                } else if(navigationState.getRawValue() == NavigationState.UNAVAILABLE) {
                    v.failure(new DroneException("Unable to navigate to goal"));
                }else if(!gpsFix.getRawValue()) {
                    v.failure(new DroneException("No GPS fix yet."));
                } else {
                    navigator.setCurrentLocation(location.getRawValue());
                    navigator.setGoal(new Location(msg.getLatitude(), msg.getLongitude(), msg.getAltitude()));
                    // navigator.setNavigationState(NavigationState.IN_PROGRESS);

                    navigationState.setValue(NavigationState.IN_PROGRESS);
                    navigationStateReason.setValue(NavigationStateReason.REQUESTED);
                    eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.IN_PROGRESS, NavigationStateReason.REQUESTED)));
                    v.success(null);
                }
            }

            // Old movetohome code:
            //moveToLocation(v, msg.getLatitude(), msg.getLongitude(), msg.getAltitude());
        }
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

            // Stop navigating
            navigator.setGoal(null);
            navigator.setCurrentLocation(null);

            // @TODO
            navigationState.setValue(NavigationState.AVAILABLE);
            navigationStateReason.setValue(NavigationStateReason.FINISHED);
            eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.AVAILABLE, NavigationStateReason.FINISHED)));

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

    protected abstract void emergency(Promise<Void> p);

    protected abstract void move3d(Promise<Void> p, double vx, double vy, double vz, double vr);

    protected abstract void moveToLocation(Promise<Void> p, double latitude, double longitude, double altitude);

    protected abstract void cancelMoveToLocation(Promise<Void> p);

    protected abstract void setMaxHeight(Promise<Void> p, float meters);

    protected abstract void setMaxTilt(Promise<Void> p, float degrees);

    protected abstract void setOutdoor(Promise<Void> p, boolean outdoor);

    protected abstract void setHull(Promise<Void> p, boolean hull);

    protected abstract void flatTrim(Promise<Void> p);

    protected abstract void reset(Promise<Void> p);

    protected abstract UnitPFBuilder<Object> createListeners();

    protected abstract LocationNavigator createNavigator(Location currentLocation, Location goal);
}
