package drones.simulation;

import akka.actor.Cancellable;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.*;
import drones.models.*;
import drones.simulation.messages.SetConnectionLostMessage;
import drones.simulation.messages.SetCrashedMessage;
import play.libs.Akka;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by yasser on 25/03/15.
 */
public class BepopSimulator extends DroneActor {

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
    protected Cancellable simulationTick;
    protected FiniteDuration simulationTimeStep = Duration.create(1, TimeUnit.SECONDS);

    protected double maxHeight;
    protected double topSpeed; // assume m/s
    protected double initialAngleWithRespectToEquator; // in radians, facing East is 0


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

            //WHY?
            //tellSelf(new TakeOffRequestMessage());
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.PENDING,
                    reason
            ));
            tellSelf(new FlyingStateChangedMessage(FlyingState.FLYING));
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.IN_PROGRESS,
                    reason
            ));
            tellSelf(new SpeedChangedMessage(10, 10, 10));
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
            //DEBUG
            System.out.println("---------------------------------");
            System.out.println("distance: " + distance);
            System.out.println("timeTillArrival: " + timeTillArrival);
            System.out.println(currentLocation.getLongtitude() + "," + currentLocation.getLatitude() + "," + currentLocation.getHeigth());
            if (timeTillArrival > timeStep) {
                // Not there yet
                double deltaLongtitude = homeLocation.getLongtitude() - currentLocation.getLongtitude();
                double deltaLatitude = homeLocation.getLatitude() - currentLocation.getLatitude();
                double deltaAltitude = homeLocation.getHeigth() - currentLocation.getHeigth();

                double fraction = timeStep/timeTillArrival;
                double newLongtitude = currentLocation.getLongtitude() + deltaLongtitude * fraction;
                double newLatitude = currentLocation.getLatitude() + deltaLatitude * fraction;
                double newHeight = currentLocation.getHeigth() + deltaAltitude * fraction;
                tellSelf(new LocationChangedMessage(
                        newLongtitude,
                        newLatitude,
                        newHeight));
            } else {
                // We have arrived
                flyingToHome = false;
                tellSelf(new LocationChangedMessage(
                        homeLocation.getLongtitude(),
                        homeLocation.getLatitude(),
                        homeLocation.getHeigth()
                ));
                tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
                tellSelf(new NavigationStateChangedMessage(
                        NavigationState.AVAILABLE,
                        NavigationStateReason.FINISHED
                ));
            }
        }
    }

    // Simulate movement based on speed
    private void simulateMovement(FiniteDuration simulationTimeStep) {

        if(!flyingToHome) {

            // Figure out angle wrt North South Axis
            double yawInRadians = rotation.getRawValue().getYaw() * Math.PI;
            double angleWrtNSAxis = initialAngleWithRespectToEquator + yawInRadians;

            // Decompose speed-x vector
            double p1 = Math.sin(angleWrtNSAxis); // = cos(angle - PI/2) = cos(angleVyWrtNS)
            double p2 = Math.cos(angleWrtNSAxis); // = -sin(angle - PI/2) = -sin(angleVyWrtNS);

            // Calculate speed along earth axises
            Speed movement = speed.getRawValue();
            double vNS = movement.getVx() * p1;
            double vEquator = movement.getVx() * p2;
            vNS += -p2 * movement.getVx();
            vEquator += p1 * movement.getVy();

            // Calculate flown distance
            double durationInSec = simulationTimeStep.toUnit(TimeUnit.SECONDS);
            double dNS = vNS * durationInSec;
            double dEquator = vEquator * durationInSec;

            // Calculate delta in radians
            double radius = Location.EARTH_RADIUS + location.getRawValue().getHeigth();
            double deltaLatitude = dNS/radius  * 180/Math.PI;          // in degrees
            double deltaLongitude = dEquator/radius  * 180/Math.PI;    // in degrees

            // Calculate new coordinates
            Location oldLocation = location.getRawValue();
            double latitude = (oldLocation.getLatitude() + deltaLatitude);    // in degrees
            if (latitude > 90) latitude = 180 -latitude;
            if (latitude < -90) latitude = Math.abs(latitude) -180;

            double longitude = (oldLocation.getLongtitude() + deltaLongitude);    // in degrees
            if (longitude > 180) longitude -= 360;
            if (longitude < -180) longitude += 360;

            Location newLocation = new Location(latitude, longitude, oldLocation.getHeigth());
            tellSelf(new LocationChangedMessage(longitude, latitude, newLocation.getHeigth()));
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

    protected void tellSelf(Object msg) {
        self().tell(msg, self());
    }

    // External control

    @Override
    protected UnitPFBuilder<Object> createListeners() {

        return ReceiveBuilder.
                match(BatteryPercentageChangedMessage.class, m -> processBatteryLevel(m.getPercent())).
                match(SetCrashedMessage.class, m -> setCrashed(m.isCrashed())).
                match(SetConnectionLostMessage.class, m -> setConnectionLost(m.isConnectionLost())).
                match(StepSimulationMessage.class, m -> stepSimulation(m.getTimeStep()));
    }

    protected void processBatteryLevel(byte percentage) {

        if(percentage < batteryLowLevel) {

            if (percentage == 0) {
                // Drone shuts down
                shutDownDrone();
            }
            else if (percentage < batteryCriticalLevel) {

                tellSelf(new AlertStateChangedMessage(AlertState.BATTERY_CRITICAL));

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
                tellSelf(new AlertStateChangedMessage(AlertState.BATTERY_LOW));
                returnToHome(NavigationStateReason.BATTERY_LOW);
            }
        }
    }

    protected void setCrashed(boolean crashed) {

        if (crashed) {
            flyingToHome = false;
            tellSelf(new AltitudeChangedMessage(0.0));
            tellSelf(new SpeedChangedMessage(0, 0, 0));
            tellSelf(new FlyingStateChangedMessage(FlyingState.EMERGENCY));
            tellSelf(new AlertStateChangedMessage(AlertState.CUT_OUT));
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.UNAVAILABLE,
                    NavigationStateReason.STOPPED
            ));
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
            returnToHome(NavigationStateReason.CONNECTION_LOST);
            tellSelf(new AlertStateChangedMessage(AlertState.CUT_OUT));
        }
        else {
            tellSelf(new AlertStateChangedMessage(AlertState.NONE));
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
    protected void emergency(Promise<Void> p) {

        if (connectionLost) return;

        //  Land
        p.failure(new NotImplementedException());
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
                tellSelf(new FlyingStateChangedMessage(FlyingState.LANDED));
                // Fall-through
            case LANDED:
                tellSelf(new FlyingStateChangedMessage(FlyingState.TAKINGOFF));
                tellSelf(new AltitudeChangedMessage(1.0));
                tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
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
                tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
                // Fall-through
            case HOVERING:
                tellSelf(new FlyingStateChangedMessage(FlyingState.LANDING));
                // Fall-through
            default:
                tellSelf(new AltitudeChangedMessage(0.0));
                tellSelf(new FlyingStateChangedMessage(FlyingState.LANDED));
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

        // 1: update rotation
        tellSelf(new AttitudeChangedMessage(vy, vx, vr));
        // 2: calculate speed resulting from the rotation
        Speed newSpeed = calculateSpeed(new Rotation(vy, vx, vr), vz);
        // 3: update the speed
        tellSelf(new SpeedChangedMessage(newSpeed.getVx(), newSpeed.getVy(), newSpeed.getVz()));
        // After processing these messages, simulateMove will have the correct behaviour

        p.failure(new NotImplementedException());
    }

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
            tellSelf(new SpeedChangedMessage(0, 0, 0));
            tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.AVAILABLE,
                    NavigationStateReason.STOPPED
            ));
            p.success(null);
        }
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
