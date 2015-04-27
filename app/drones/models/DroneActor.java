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
import drones.messages.*;
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
    protected LazyProperty<Boolean> flatTrimStatus;
    protected LazyProperty<Rotation> rotation;
    protected LazyProperty<Speed> speed;
    protected LazyProperty<Double> altitude;
    protected LazyProperty<DroneVersion> version;
    protected LazyProperty<NavigationState> navigationState;
    protected LazyProperty<NavigationStateReason> navigationStateReason;
    protected LazyProperty<Boolean> gpsFix;
    protected LazyProperty<Boolean> isOnline;
    protected LazyProperty<Boolean> calibrationRequired;
    protected LazyProperty<byte[]> image;

    protected DroneEventBus eventBus;

    private boolean loaded = false;
    private boolean loading = false;

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public DroneActor() {
        eventBus = new DroneEventBus(self());

        batteryPercentage = new LazyProperty<>();
        state = new LazyProperty<>(FlyingState.LANDED); //TODO: check assumption of connecting in-flight
        alertState = new LazyProperty<>(AlertState.NONE);
        flatTrimStatus = new LazyProperty<>(false);
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
        image = new LazyProperty<>();

        // TODO: build pipeline that directly forwards to the eventbus
        //TODO: revert quickfix and support null
        UnitPFBuilder<Object> extraListeners = createListeners();
        if (extraListeners == null) { // When null, create a new listener chain
            extraListeners = ReceiveBuilder.match(PropertyRequestMessage.class, this::handlePropertyRequest);
        } else {
            extraListeners = extraListeners.match(PropertyRequestMessage.class, this::handlePropertyRequest);
        }
        receive(extraListeners.
                match(StopMessage.class, s -> stop()).
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
                match(FlipRequestMessage.class, s -> flipInternal(sender(), self(), s.getFlip())).
                match(InitVideoMessage.class, s -> initVideoInternal(sender(), self())).
                match(SubscribeEventMessage.class, s -> handleSubscribeMessage(sender(), s.getSubscribedClasses())).
                match(UnsubscribeEventMessage.class, s -> handleUnsubscribeMessage(sender(), s.getSubscribedClass())).

                // Drone -> external
                match(LocationChangedMessage.class, s -> setLocation(new Location(s.getLatitude(), s.getLongitude(), s.getGpsHeight()))).
                match(GPSFixChangedMessage.class, s -> setGPSFix(s.isFixed())).
                match(BatteryPercentageChangedMessage.class, s -> setBatteryPercentage(s.getPercent())).
                match(FlyingStateChangedMessage.class, s -> setFlyingState(s.getState())).
                match(AlertStateChangedMessage.class, s -> setAlertState(s.getState())).
                match(FlatTrimChangedMessage.class, s -> setFlatTrim(true)).
                match(RotationChangedMessage.class, s -> setRotation(new Rotation(s.getRoll(), s.getPitch(), s.getYaw()))).
                match(AltitudeChangedMessage.class, s -> setAltitude(s.getAltitude())).
                match(SpeedChangedMessage.class, s -> setSpeed(new Speed(s.getSpeedX(), s.getSpeedY(), s.getSpeedZ()))).
                match(ProductVersionChangedMessage.class, s -> setProductVersion(new DroneVersion(s.getSoftware(), s.getHardware()))).
                match(NavigationStateChangedMessage.class, s -> setNavigationState(s.getState(), s.getReason())).
                match(MagnetoCalibrationStateChangedMessage.class, s -> setMagnetoCalibrationState(s.isCalibrationRequired())).
                match(ConnectionStatusChangedMessage.class, s -> setConnectionStatus(s.isConnected())).
                match(JPEGFrameMessage.class, s -> setJPEGImage(s.getByteData())).
                matchAny(o -> log.info("DroneActor unk message recv: [{}]", o.getClass().getCanonicalName())).build());
    }

    protected void setLocation(Location l) {
        location.setValue(l);
        eventBus.publish(new DroneEventMessage(
                new LocationChangedMessage(l.getLongitude(), l.getLatitude(), l.getHeight())));
    }

    protected void setGPSFix(boolean fix) {
        log.info("GPS fix changed: [{}]", fix);
        gpsFix.setValue(fix);
        eventBus.publish(new DroneEventMessage(new GPSFixChangedMessage(fix)));
    }

    protected void setFlyingState(FlyingState s) {
        state.setValue(s);
        eventBus.publish(new DroneEventMessage(new FlyingStateChangedMessage(s)));
    }

    protected void setAlertState(AlertState state) {
        alertState.setValue(state);
        eventBus.publish(new DroneEventMessage(new AlertStateChangedMessage(state)));
    }

    protected void setFlatTrim(boolean trimmed){
        flatTrimStatus.setValue(trimmed);
        eventBus.publish(new DroneEventMessage(new FlatTrimChangedMessage()));
    }

    protected void setBatteryPercentage(byte percentage) {
        batteryPercentage.setValue(percentage);
        eventBus.publish(new DroneEventMessage(new BatteryPercentageChangedMessage(percentage)));
    }

    protected void setRotation(Rotation rot){
        rotation.setValue(rot);
        eventBus.publish(
                new DroneEventMessage(new RotationChangedMessage(rot.getRoll(), rot.getPitch(), rot.getYaw())));
    }

    protected void setAltitude(double a){
        altitude.setValue(a);
        eventBus.publish(new DroneEventMessage(new AltitudeChangedMessage(a)));
    }

    protected void setSpeed(Speed s){
        speed.setValue(s);
        eventBus.publish(
                new DroneEventMessage(new SpeedChangedMessage(s.getVx(), s.getVy(), s.getVz())));
    }

    protected void setProductVersion(DroneVersion v){
        version.setValue(v);
        eventBus.publish(new DroneEventMessage(new ProductVersionChangedMessage(v.getSoftware(), v.getHardware())));
    }

    protected void setNavigationState(NavigationState state, NavigationStateReason reason){
        navigationState.setValue(state);
        navigationStateReason.setValue(reason);
        eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(state, reason)));
    }

    protected void setMagnetoCalibrationState(boolean calibRequired){
        calibrationRequired.setValue(calibRequired);
        eventBus.publish(new DroneEventMessage(new MagnetoCalibrationStateChangedMessage(calibRequired)));

        if (calibRequired)
            log.warning("Drone requires calibration!!!");
        else
            log.info("No drone calibration required.");
    }

    protected void setConnectionStatus(boolean connected){
        isOnline.setValue(connected);
        eventBus.publish(new DroneEventMessage(new ConnectionStatusChangedMessage(connected)));

        if (!connected) {
            log.warning("Drone network became unreachable.");
        } else {
            log.info("Drone network became reachable.");
        }
    }

    protected void setJPEGImage(byte[] jpegImageData) {
        image.setValue(jpegImageData);
        eventBus.publish(new DroneEventMessage(new JPEGFrameMessage(jpegImageData)));
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.create("1 minute"),
                t -> {
                    log.error(t, "DroneActor failure caught by supervisor.");
                    System.err.println(t.getMessage());
                    return SupervisorStrategy.resume(); // Continue on all exceptions!
                });
    }

    private void handleSubscribeMessage(final ActorRef sub, Class[] cl) {
        for (Class c : cl) {
            eventBus.subscribe(sub, c);
        }
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
            case IMAGE:
                handleMessage(image.getValue(), sender(), self());
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

    protected void cancelMoveToLocationInternal(final ActorRef sender, final ActorRef self) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone cannot move when not initialized")), self);
        } else {
            log.info("Cancelling move to location.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            cancelMoveToLocation(v);
        }
    }

    protected void moveToLocationInternal(final ActorRef sender, final ActorRef self, final MoveToLocationRequestMessage msg) {
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Drone cannot move when not initialized")), self);
        } else {
            log.info("Navigating to lat=[{}], long=[{}], alt=[{}]", msg.getLatitude(), msg.getLongitude(), msg.getAltitude());
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            moveToLocation(v, msg.getLatitude(), msg.getLongitude(), msg.getAltitude());
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

    protected void landInternal(final ActorRef sender, final ActorRef self) {
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

    private void flipInternal(final ActorRef sender, final ActorRef self, FlipType type){
        if (!loaded || state.getRawValue() == FlyingState.LANDED) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Cannot flip when on ground / not initialized.")), self);
        } else {
            log.debug("Attempting flip.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            flip(v, type);
        }
    }

    private void initVideoInternal(final ActorRef sender, final ActorRef self){
        if (!loaded) {
            sender.tell(new akka.actor.Status.Failure(new DroneException("Cannot init video when not initialized.")), self);
        } else {
            log.debug("Attempting init video.");
            Promise<Void> v = Futures.promise();
            handleMessage(v.future(), sender, self);
            initVideo(v);
        }
    }

    protected abstract void stop();

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

    protected abstract void flip(Promise<Void> p, FlipType type);

    protected abstract void initVideo(Promise<Void> p);

    protected abstract UnitPFBuilder<Object> createListeners();
}
