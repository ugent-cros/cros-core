package simulation;

import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.*;
import drones.models.*;
import scala.concurrent.Promise;
import simulation.messages.SetConnectionLostMessage;
import simulation.messages.SetCrashedMessage;

/**
 * Created by yasser on 25/03/15.
 */
public class DroneActorSimulator extends DroneActor {

    private byte batteryLowLevel = 10;
    private byte batteryCriticalLevel = 5;

    private boolean initialized = false;
    private boolean crashed = false;
    private boolean connectionLost = false;

    private double maxHeight;

    // TODO: settable delay, sleep/suspend

    // Control stuff

    @Override
    protected UnitPFBuilder<Object> createListeners() {

        // TODO: create test messages for crash, low batter, out of range, …

        ReceiveBuilder.
                match(BatteryPercentageChangedMessage.class, m -> {
                    if(m.getPercent() < batteryLowLevel) {
                        if(m.getPercent() < batteryCriticalLevel) {
                            tellSelf(new AlertStateChangedMessage(AlertState.BATTERY_CRITICAL));
                            tellSelf(new LandRequestMessage());
                            tellSelf(new NavigationStateChangedMessage(
                                    NavigationState.UNAVAILABLE,
                                    NavigationStateReason.BATTERY_LOW));
                        } else {
                            tellSelf(new AlertStateChangedMessage(AlertState.BATTERY_LOW));
                        }
                    }
                }).
                match(SetCrashedMessage.class, m -> setCrashed(m.isCrashed())).
                match(SetConnectionLostMessage.class, m -> setConnectionLost(m.isConnectionLost()));

        return null;
    }

    protected void setCrashed(boolean crashed) {

        if (crashed) {
            tellSelf(new AltitudeChangedMessage(0.0));
            tellSelf(new FlyingStateChangedMessage(FlyingState.EMERGENCY));
            tellSelf(new AlertStateChangedMessage(AlertState.USER_EMERGENCY));
            tellSelf(new SpeedChangedMessage(0, 0, 0));
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.UNAVAILABLE,
                    NavigationStateReason.STOPPED
            ));
        }
        else {
            tellSelf(new FlyingStateChangedMessage(FlyingState.LANDED));
            tellSelf(new AlertStateChangedMessage(AlertState.NONE));
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.AVAILABLE,
                    NavigationStateReason.ENABLED
            ));
        }
        this.crashed = crashed;
    }

    protected void setConnectionLost(boolean connectionLost) {

        if(connectionLost) {
            // Don't tell self (we don't want this messages published as we are out of range
            alertState.setValue(AlertState.CUT_OUT);
            navigationState.setValue(NavigationState.UNAVAILABLE);
            navigationStateReason.setValue(NavigationStateReason.CONNECTION_LOST);
        }
        else {
            tellSelf(new AlertStateChangedMessage(AlertState.NONE));
            tellSelf(new NavigationStateChangedMessage(
                    NavigationState.AVAILABLE,
                    NavigationStateReason.ENABLED
            ));
        }
        this.connectionLost = connectionLost;
    }

    // Utility methods
    protected void tellSelf(Object msg) {
        self().tell(msg, self());
    }

    public DroneActorSimulator() {
        state.setValue(FlyingState.LANDED);
        alertState.setValue(AlertState.NONE);
        location.setValue(new Location(0.0, 0.0, 0.0));
        batteryPercentage.setValue((byte) 100);
        rotation.setValue(new Rotation(0, 0, 0));
        speed.setValue(new Speed(0.0, 0.0, 0.0));
        altitude.setValue(0.0);
        version.setValue(new DroneVersion("1.0", "1.0"));
        navigationState.setValue(NavigationState.AVAILABLE);
        navigationStateReason.setValue(NavigationStateReason.ENABLED);
        gpsFix.setValue(false);

        maxHeight = 10;
    }

    // Implementation of DroneActor
    @Override
    protected void init(Promise<Void> p) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
            return;
        }

        initialized = true;
        p.success(null);
    }

    @Override
    protected void takeOff(Promise<Void> p) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
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
                // Fall-throug
            default:
                // TODO: set navigation status?
                p.success(null);
        }
    }

    @Override
    protected void land(Promise<Void> p) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
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
            p.failure(new DroneException("Drone is unreachable"));
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
            p.failure(new DroneException("Drone is unreachable"));
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
            tellSelf(new FlyingStateChangedMessage(FlyingState.FLYING));
            // TODO: unit of speed? m/s km/h
            tellSelf(new SpeedChangedMessage(10, 10, 10));
            double height = Math.min(altitude, maxHeight);
            LocationChangedMessage locationMsg = new LocationChangedMessage(longitude, latitude, height);
            tellSelf(locationMsg);
            // TODO: simulate battery usage?
            tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));

            p.success(null);
        } else {
            p.failure(new DroneException("Drone is unable to move in current state: "
                    + flyingState.name()));
        }
    }

    @Override
    protected void cancelMoveToLocation(Promise<Void> p) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
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
            tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
            p.success(null);
        }
    }

    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
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
            p.failure(new DroneException("Drone is unreachable"));
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
            p.failure(new DroneException("Drone is unreachable"));
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

        // no effect on simulation
        p.success(null);
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
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

        // No effect on simulation
        p.success(null);
    }

    @Override
    protected void flatTrim(Promise<Void> p) {

        if(connectionLost) {
            p.failure(new DroneException("Drone is unreachable"));
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
