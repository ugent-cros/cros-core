package drones.simulation;

import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.*;
import drones.models.*;
import play.libs.Akka;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import drones.simulation.messages.SetConnectionLostMessage;
import drones.simulation.messages.SetCrashedMessage;

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
    protected FiniteDuration simulationTimeStep = Duration.create(1, TimeUnit.SECONDS);

    protected double maxHeight;
    protected double topSpeed; // assume m/s

    // internal state
    private boolean crashed = false;
    private boolean connectionLost = false;

    private boolean initialized = false;

    private Location homeLocation;
    private boolean flyingToHome;

    // Control stuff

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
            tellSelf(new AlertStateChangedMessage(AlertState.USER_EMERGENCY));
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

    private void returnToHome(NavigationStateReason reason) {

        // Check if drone is not already flying home
        if(!flyingToHome && !crashed) {

            flyingToHome = true;

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
            double distance = Location.distance(currentLocation, homeLocation)/1000;  // km/1000 -> m
            double timeTillArrival = distance/topSpeed;
            double timeStep = timeFlown.toUnit(TimeUnit.SECONDS);

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
                tellSelf(new LocationChangedMessage(
                        homeLocation.getLongtitude(),
                        homeLocation.getLatitude(),
                        homeLocation.getHeigth()
                ));
            }

            // Check if we have arrived ore not
            if (timeTillArrival < timeStep) {
                // We have arrived
                flyingToHome = false;
                // Update state
                tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
                tellSelf(new NavigationStateChangedMessage(
                        NavigationState.AVAILABLE,
                        NavigationStateReason.FINISHED
                ));
            }
        }
    }

    protected void tellSelf(Object msg) {
        self().tell(msg, self());
    }

    public void rebootDrone() {

        if(batteryPercentage.getRawValue() > 0) {
            state.setValue(FlyingState.LANDED);
            alertState.setValue(AlertState.NONE);
            speed.setValue(new Speed(0.0, 0.0, 0.0));
            altitude.setValue(0.0);
            navigationState.setValue(NavigationState.AVAILABLE);
            navigationStateReason.setValue(NavigationStateReason.ENABLED);

            flyingToHome = false;
            initialized = false;
        }
    }

    public void shutDownDrone() {
        flyingToHome = false;
        setConnectionLost(true);
        initialized = false;
    }

    public BepopSimulator() {

        location.setValue(new Location(0.0, 0.0, 0.0));
        batteryPercentage.setValue((byte) 100);
        rotation.setValue(new Rotation(0, 0, 0));
        version.setValue(new DroneVersion("1.0", "1.0"));
        gpsFix.setValue(false);

        maxHeight = 10;
        topSpeed = 10;

        rebootDrone();

        // Schedule drones.simulation loop
        Akka.system().scheduler().schedule(
                Duration.Zero(),
                simulationTimeStep,
                self(),
                new StepSimulationMessage(simulationTimeStep),
                Akka.system().dispatcher(),
                self());
    }

    // Implementation of DroneActor
    @Override
    protected void init(Promise<Void> p) {

        if(connectionLost) {
            return;
        }

        initialized = true;
        p.success(null);
    }

    @Override
    protected void takeOff(Promise<Void> p) {

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
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

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
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

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return;
        }

        // TODO: need specification of this method
        p.success(null);
    }

    @Override
    protected void moveToLocation(Promise<Void> p, double latitude, double longitude, double altitude) {

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
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

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
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

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        /* TODO: necessary here?
        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return;
        }
        */

        maxHeight = meters;
        p.success(null);
    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        /* TODO: necessary here?
        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return;
        }
        */

        p.success(null);
    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        /* TODO: necessary here?
        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return;
        }
        */

        // no effect on drones.simulation
        p.success(null);
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        /* TODO: necessary here?
        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return;
        }
        */

        // No effect on drones.simulation
        p.success(null);
    }

    @Override
    protected void flatTrim(Promise<Void> p) {

        if(connectionLost) {
            return;
        }

        if(!initialized) {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
            return;
        }

        /* TODO: necessary here?
        if(crashed) {
            p.failure(new DroneException("Drone crashed, unable to execute commands"));
            return;
        }
        */

        // TODO: find out what this should do
        p.success(null);
    }
}
