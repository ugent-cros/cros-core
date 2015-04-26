package drones.simulation;

import akka.actor.Cancellable;
import akka.dispatch.Futures;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.BatteryPercentageChangedMessage;
import drones.models.*;
import drones.simulation.messages.ResetMovementMessage;
import drones.simulation.messages.SetConnectionLostMessage;
import drones.simulation.messages.SetCrashedMessage;
import drones.util.LocationNavigator;
import play.libs.Akka;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by yasser on 25/03/15.
 */
public class BepopSimulator extends NavigatedDroneActor {

    private static class StepSimulationMessage implements Serializable {
        private FiniteDuration timeStep;
        public StepSimulationMessage(FiniteDuration timeStep) {
            this.timeStep = timeStep;
        }
        public FiniteDuration getTimeStep() { return timeStep; }
    }

    // Simulator properties

    // TODO: make drones.simulation properties settable
    protected byte batteryLowLevel = 10;
    protected byte batteryCriticalLevel = 5;
    protected FiniteDuration simulationTimeStep = Duration.create(1, TimeUnit.SECONDS);

    protected double maxHeight;
    protected double topSpeed; // assume m/s
    protected double initialAngleWithRespectToEquator; // in radians, facing East is 0

    // Private variables needed for simulation
    private Cancellable simulationTick;
    private Cancellable resetDefaultMovement;

    // internal state
    private boolean crashed = false;
    private boolean connectionLost = false;

    private boolean initialized = false;

    private Location homeLocation;
    private boolean flyingToHome;

    // angleWrtEquator: in radians
    public BepopSimulator(Location startLocation, double maxHeight, double angleWrtEquator, double topSpeed) {

        // Set initial values
        location.setValue(startLocation);
        batteryPercentage.setValue((byte) 100);
        rotation.setValue(new Rotation(0, 0, 0));
        version.setValue(new DroneVersion("1.0", "1.0"));
        gpsFix.setValue(false);

        // Set simulation values
        this.maxHeight = maxHeight;
        this.topSpeed = topSpeed;
        initialAngleWithRespectToEquator = angleWrtEquator; // facing north

        // Disable messages before initialization
        eventBus.setPublishDisabled(true);

        // Setup drone
        bootDrone();
    }

    // Utility methods

    public void rebootDrone() {

        shutDownDrone();
        bootDrone();
    }

    public void bootDrone() {

        // Set default values
        state.setValue(FlyingState.LANDED);
        alertState.setValue(AlertState.NONE);
        speed.setValue(new Speed(0.0, 0.0, 0.0));
        altitude.setValue(0.0);
        navigationState.setValue(NavigationState.AVAILABLE);
        navigationStateReason.setValue(NavigationStateReason.ENABLED);
        gpsFix.setValue(true);
    }

    public void shutDownDrone() {

        // Cancel everything: drone is powered off
        simulationTick.cancel();
        flyingToHome = false;
        eventBus.setPublishDisabled(true);
        setConnectionLost(true);
        initialized = false;
    }

    private void returnToHome(NavigationStateReason reason) {

        // Check if drone is not already flying home
        if(!flyingToHome && !crashed) {

            flyingToHome = true;
            setNavigationState(NavigationState.PENDING, reason);
            setFlyingState(FlyingState.FLYING);
            setNavigationState(NavigationState.IN_PROGRESS, reason);
            setSpeed(new Speed(10, 0, 0));
        }
    }

    private void stepSimulation(FiniteDuration stepDuration) {

        // Move
        simulateMovement(stepDuration);

        // Fly further
        progressFlight(stepDuration);

        // TODO: send update to eventbus of all properties that periodically update
    }

    private void progressFlight(FiniteDuration timeFlown) {

        // Check if moveToLocation wasn't cancelled
        // If flying is aborted, the states, reasons & other properties
        // should be set in the caller that set this flag to false
        if(flyingToHome) {
            Location currentLocation = location.getRawValue();

            // Calculate distance
            double distance = Location.distance(currentLocation, homeLocation);  // m
            double timeTillArrival = distance/topSpeed;
            double timeStep = timeFlown.toUnit(TimeUnit.SECONDS);
            if (timeTillArrival > timeStep) {
                // Not there yet
                double deltaLongitude = homeLocation.getLongitude() - currentLocation.getLongitude();
                double deltaLatitude = homeLocation.getLatitude() - currentLocation.getLatitude();
                double deltaAltitude = homeLocation.getHeight() - currentLocation.getHeight();

                double fraction = timeStep/timeTillArrival;
                double newLongitude = currentLocation.getLongitude() + deltaLongitude * fraction;
                double newLatitude = currentLocation.getLatitude() + deltaLatitude * fraction;
                double newHeight = currentLocation.getHeight() + deltaAltitude * fraction;
                setLocation(new Location(newLatitude, newLongitude, newHeight));

            } else {
                // We have arrived
                flyingToHome = false;

                setLocation(homeLocation);
                setSpeed(new Speed(0, 0, 0));
                setFlyingState(FlyingState.HOVERING);
                setNavigationState(NavigationState.AVAILABLE, NavigationStateReason.FINISHED);
            }
        }
    }

    // Simulate movement based on speed
    private void simulateMovement(FiniteDuration simulationTimeStep) {

        if(!flyingToHome) {

            Speed movement = speed.getRawValue();

            // Simple stuff first: update height
            double deltaHeight = 1 * movement.getVz();
            double updatedAltitude = Math.max(0, altitude.getRawValue() + deltaHeight);
            setAltitude(updatedAltitude);

            // Figure out angle wrt North South Axis
            double yawInRadians = rotation.getRawValue().getYaw();
            double angleWrtEquator = initialAngleWithRespectToEquator + yawInRadians;

            // Decompose speed-x vector
            double p1 = Math.sin(angleWrtEquator); // = cos(angle - PI/2) = cos(angleVyWrtEquator)
            double p2 = Math.cos(angleWrtEquator); // = -sin(angle - PI/2) = -sin(angleVyWrtEquator);

            // Calculate speed along earth axises
            double vNS = movement.getVx() * p1;
            double vEquator = movement.getVx() * p2;

            // Add speed-y vector
            vNS += movement.getVy() * -p2;
            vEquator += movement.getVy() * p1;

            // Calculate flown distance
            double durationInSec = simulationTimeStep.toUnit(TimeUnit.SECONDS);
            double dNS = vNS * durationInSec;
            double dEquator = vEquator * durationInSec;

            // Calculate delta in radians
            double radius = Location.EARTH_RADIUS + location.getRawValue().getHeight();
            double deltaLatitude = dNS/radius  * 180/Math.PI;          // in degrees
            double deltaLongitude = dEquator/radius  * 180/Math.PI;    // in degrees

            // Calculate new coordinates
            Location oldLocation = location.getRawValue();
            double latitude = (oldLocation.getLatitude() + deltaLatitude);    // in degrees
            if (latitude > 90) latitude = 180 -latitude;
            if (latitude < -90) latitude = Math.abs(latitude) -180;

            double longitude = (oldLocation.getLongitude() + deltaLongitude);    // in degrees

            if (longitude > 180) longitude -= 360;
            if (longitude < -180) longitude += 360;

            setLocation(new Location(latitude, longitude, updatedAltitude));
        }
    }

    // Calculates speed corresponding with a give rotation
    private Speed calculateSpeed(Rotation rotation, double vz) {

        // Getting necessary info
        double pitch = rotation.getPitch();
        double roll = rotation.getRoll();

        // Calculate speed components
        double xFraction, yFraction;

        if (roll != 0) {
            // Calculate flying angle
            double angle = Math.atan(pitch / roll);
            xFraction = Math.sin(angle)*pitch;
            yFraction = Math.cos(angle)*roll;
        }
        else {
            // We know the flying angle
            xFraction = 1*pitch;
            yFraction = 0*roll;
        }

        double vx = xFraction * topSpeed;
        double vy = yFraction * topSpeed;

        return new Speed(vx, vy, vz);
    }

    // External control

    @Override
    protected UnitPFBuilder<Object> createListeners() {

        return ReceiveBuilder.
                match(BatteryPercentageChangedMessage.class, m -> processBatteryLevel(m.getPercent())).
                match(SetCrashedMessage.class, m -> setCrashed(m.isCrashed())).
                match(SetConnectionLostMessage.class, m -> setConnectionLost(m.isConnectionLost())).
                match(StepSimulationMessage.class, m -> stepSimulation(m.getTimeStep())).
                match(ResetMovementMessage.class, s -> {
                    Promise p = Futures.promise();
                    move3d(p, 0, 0, 0, 0);
                });
    }

    @Override
    public void setRotation(Rotation rot) {
        super.setRotation(rot);
        // Update speed according to rotation
        Speed updatedSpeed = calculateSpeed(rot, speed.getRawValue().getVz());
        setSpeed(updatedSpeed);
    }

    @Override
    public void setSpeed(Speed speed) {
        super.setSpeed(speed);

        if (state.getRawValue() == FlyingState.FLYING
                && Math.abs(speed.getVx()) + Math.abs(speed.getVy()) + Math.abs(speed.getVz()) == 0) {
            setFlyingState(FlyingState.HOVERING);
        }
        else if (state.getRawValue() == FlyingState.HOVERING
                && Math.abs(speed.getVx()) + Math.abs(speed.getVy()) + Math.abs(speed.getVz()) > 0) {
            setFlyingState(FlyingState.FLYING);
        }
    }

    @Override
    protected void stop() {

    }

    protected void processBatteryLevel(byte percentage) {

        if(percentage < batteryLowLevel) {

            if (percentage == 0) {
                // Drone shuts down
                shutDownDrone();
            }
            else if (percentage < batteryCriticalLevel) {

                setAlertState(AlertState.BATTERY_CRITICAL);

                // TODO: figure out what happens

                /*
                tellSelf(new LandRequestMessage());
                tellSelf(new NavigationStateChangedMessage(
                        NavigationState.UNAVAILABLE,
                        NavigationStateReason.BATTERY_LOW));
                */
            }
            else {
                // Return to home on low battery
                setAlertState(AlertState.BATTERY_LOW);
                //returnToHome(NavigationStateReason.BATTERY_LOW);
            }
        }
    }

    protected void setCrashed(boolean crashed) {

        if (crashed) {
            flyingToHome = false;

            setAltitude(0);
            setSpeed(new Speed(0, 0, 0));
            setFlyingState(FlyingState.EMERGENCY);
            setAlertState(AlertState.CUT_OUT);
            setNavigationState(NavigationState.UNAVAILABLE, NavigationStateReason.STOPPED);
        }
        else {
            rebootDrone();
        }
        this.crashed = crashed;
    }

    protected void setConnectionLost(boolean connectionLost) {

        // Disable/re-enable event bus
        eventBus.setPublishDisabled(connectionLost);

        if(connectionLost) {

            // Should start navigation to home
            // returnToHome(NavigationStateReason.CONNECTION_LOST);
            setAlertState(AlertState.CUT_OUT);
        }
        else {
            setAlertState(AlertState.NONE);
        }
        this.connectionLost = connectionLost;
    }

    // Implementation of DroneActor

    private <T> boolean prematureExit(Promise<T> p) {
        if(connectionLost) {
            return true;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return true;
        }

        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return true;
        }
        return false;
    }

    @Override
    protected void init(Promise<Void> p) {

        if(connectionLost) {
            return;
        }

        // Enable status updates
        eventBus.setPublishDisabled(false);

        // Schedule drones.simulation loop
        simulationTick = Akka.system().scheduler().schedule(
                simulationTimeStep,
                simulationTimeStep,
                self(),
                new StepSimulationMessage(simulationTimeStep),
                Akka.system().dispatcher(),
                self());

        initialized = true;
        p.success(null);
    }

    @Override
    protected void reset(Promise<Void> p) {

        if(connectionLost) return;

        rebootDrone();
        init(p);
    }

    @Override
    protected void flip(Promise<Void> p, FlipType type) {
        p.failure(new DroneException("Not implemented yet."));
    }

    @Override
    protected void emergency(Promise<Void> p) {

        if (connectionLost) return;

        //  Land
        p.failure(new DroneException("action not implemented"));
    }

    @Override
    protected void takeOff(Promise<Void> p) {

        if (prematureExit(p)) {
            return;
        }

        switch (state.getRawValue()) {
            case EMERGENCY:
                p.failure(new DroneException("Drone is in emergency status"));
                break;
            case LANDING:
                // Land drone before taking off again
                setFlyingState(FlyingState.LANDED);
                // Fall-through
            case LANDED:
                setFlyingState(FlyingState.TAKINGOFF);
                setAltitude(1);
                setFlyingState(FlyingState.HOVERING);
                // Fall-through
            default:
                p.success(null);
        }
    }

    @Override
    protected void land(Promise<Void> p) {

        if (prematureExit(p)) {
            return;
        }

        flyingToHome = false;
        switch (state.getRawValue()) {
            case EMERGENCY:
                p.failure(new DroneException("Drone is in emergency status"));
                break;
            case TAKINGOFF:
                // Hover drone before landing again
                setFlyingState(FlyingState.HOVERING);
                // Fall-through
            case HOVERING:
                setFlyingState(FlyingState.LANDING);
                // Fall-through
            default:
                setAltitude(0);
                setFlyingState(FlyingState.LANDED);
                p.success(null);
        }
    }

    @Override
    protected void move3d(Promise<Void> p, double vx, double vy, double vz, double vr) {

        if (prematureExit(p)) {
            return;
        }

        // Drone is flying to home
        if (flyingToHome) {
            p.failure(new DroneException("Drone is flying to home, first cancel move to location"));
        }

        // Check if arguments are valid
        if (Math.abs(vx) > 1 || Math.abs(vy) > 1 || Math.abs(vz) > 1 || Math.abs(vr) > 1) {
            p.failure(new DroneException("Invalid arguments: vx, vy, vz and vr need to be in [-1, 1]"));
        }

        // Cancel setting the rotation back to normal if a new move message arrives
        if (resetDefaultMovement != null) {
            resetDefaultMovement.cancel();
        }

        // Update vz
        Speed rawSpeed = speed.getRawValue();
        setSpeed(new Speed(rawSpeed.getVx(), rawSpeed.getVy(), vz));

        // Calculate rotation
        double roll = vy * Math.PI/3;   // 1 <-> 60°
        double pitch = vx * Math.PI/3;  // 1 <-> 60°
        double deltaYaw = -vr * Math.PI/6;    // 1 <-> turn of 30°
        double yaw = rotation.getRawValue().getYaw() + deltaYaw;

        // Update rotation: this will also update the speed
        // Next simulation step will use the updated speed values
        setRotation(new Rotation(roll, pitch, yaw));

        // After a 1.5 second: the rotation should be set back to normal
        resetDefaultMovement = Akka.system().scheduler().scheduleOnce(
                Duration.create(1500, TimeUnit.MILLISECONDS),   // At least 1 simulation step will have executed
                self(),
                new ResetMovementMessage(),
                Akka.system().dispatcher(),
                self()
        );


        p.success(null);
    }

    /*
    @Override
    protected void moveToLocation(Promise<Void> p, double latitude, double longitude, double altitude) {

        if (prematureExit(p)) {
            return;
        }

        FlyingState flyingState = state.getRawValue();
        if(flyingState == FlyingState.FLYING || flyingState == FlyingState.HOVERING) {

            // Set new home location & fly towards it
            double height = Math.min(altitude, maxHeight);
            homeLocation = new Location(latitude, longitude, height);
            returnToHome(NavigationStateReason.REQUESTED);

            p.success(null);
        } else {
            p.failure(new DroneException("Drone is unable to move in current state: "
                    + flyingState.name()));
        }
    }

    @Override
    protected void cancelMoveToLocation(Promise<Void> p) {

        if (prematureExit(p)) {
            return;
        }

        if(state.getRawValue() == FlyingState.EMERGENCY) {
            p.failure(new DroneException("Unable to send commands to drone in emergency state"));
        } else {
            flyingToHome = false;
            setSpeed(new Speed(0, 0, 0));
            setFlyingState(FlyingState.HOVERING);
            setNavigationState(NavigationState.AVAILABLE, NavigationStateReason.STOPPED);
            p.success(null);
        }
    }
    */

    @Override
    protected LocationNavigator createNavigator(Location currentLocation, Location goal) {
        return new LocationNavigator(currentLocation, goal, (float)topSpeed,  30f, 1f); // Bebop parameters
    }


    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {

        if (prematureExit(p)) {
            return;
        }

        maxHeight = meters;
        p.success(null);
    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {

        if (prematureExit(p)) {
            return;
        }

        p.success(null);
    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {

        if (prematureExit(p)) {
            return;
        }

        // no effect on drones.simulation
        p.success(null);
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {

        if (prematureExit(p)) {
            return;
        }

        // No effect on drones.simulation
        p.success(null);
    }

    @Override
    protected void flatTrim(Promise<Void> p) {

        if (prematureExit(p)) {
            return;
        }

        // TODO: find out what this should do
        p.success(null);
    }
}
