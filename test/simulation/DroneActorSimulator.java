package simulation;

import akka.japi.pf.UnitPFBuilder;
import drones.messages.AltitudeChangedMessage;
import drones.messages.FlyingStateChangedMessage;
import drones.messages.LocationChangedMessage;
import drones.messages.SpeedChangedMessage;
import drones.models.*;
import scala.concurrent.Promise;

/**
 * Created by yasser on 25/03/15.
 */
public class DroneActorSimulator extends DroneActor {

    private boolean initialized = false;
    private double maxHeight;

    // TODO: settable delay, sleep/suspend

    // Control stuff

    // Utility method
    protected void tellSelf(Object msg) {
        self().tell(msg, self());
    }

    // Implementation of DroneActor
    @Override
    protected void init(Promise<Void> p) {

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
        initialized = true;

        p.success(null);
    }

    @Override
    protected void takeOff(Promise<Void> p) {

        if(initialized) {

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
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void land(Promise<Void> p) {

        if(initialized) {

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
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void move3d(Promise<Void> p, double vx, double vy, double vz, double vr) {
        if(initialized) {
            // TODO: need specification of this method
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void moveToLocation(Promise<Void> p, double latitude, double longitude, double altitude) {

        if(initialized) {

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
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void cancelMoveToLocation(Promise<Void> p) {
        if(initialized) {

            if(state.getRawValue() == FlyingState.EMERGENCY) {
                p.failure(new DroneException("Unable to send commands to drone in emergency state"));
            } else {
                tellSelf(new FlyingStateChangedMessage(FlyingState.HOVERING));
                p.success(null);
            }
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {
        if(initialized) {
            maxHeight = meters;
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {
        if(initialized) {
            // TODO: do something with this?
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {
        if(initialized) {
            // no effect on simulation
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {
        if(initialized) {
            // No effect on simulation
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected void flatTrim(Promise<Void> p) {
        if(initialized) {
            // TODO: find out what this should do
            p.success(null);
        } else {
            p.failure(new DroneException("Failed to send command. Not initialized yet."));
        }
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {

        // TODO: create test messages for crash, low batter, out of range, …

        return null;
    }
}
