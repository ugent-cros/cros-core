package droneapi.model;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;
import droneapi.messages.NavigationStateChangedMessage;
import droneapi.model.properties.Location;
import droneapi.model.properties.NavigationState;
import droneapi.model.properties.NavigationStateReason;
import droneapi.navigator.LocationNavigator;
import droneapi.navigator.MoveVector;
import scala.concurrent.Promise;

/**
 * Created by Cedric on 4/21/2015.
 */
public abstract class NavigatedDroneActor extends DroneActor {
    // Navigation
    private LocationNavigator navigator;
    private final Object navigationLock;

    public NavigatedDroneActor() {
        super();
        navigationLock = new Object();
    }

    @Override
    protected void setGPSFix(boolean fix) {
        // Publish navigation state to event bus
        if (fix && !gpsFix.getRawValue()) {
            navigationState.setValue(NavigationState.AVAILABLE);
            eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.AVAILABLE, NavigationStateReason.ENABLED)));
        } else if (!fix && gpsFix.getRawValue()) {
            navigationState.setValue(NavigationState.UNAVAILABLE);
            eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.UNAVAILABLE, NavigationStateReason.CONNECTION_LOST)));
        }
        super.setGPSFix(fix);
    }

    @Override
    protected void setLocation(Location l) {
        super.setLocation(l);
        processLocation(l);
    }

    protected void processLocation(Location location) {
        synchronized (navigationLock) {
            if (navigationState.getRawValue() != NavigationState.IN_PROGRESS)
                return;

            // When there's no gps fix, continue
            if (!gpsFix.getRawValue()) {
                // Stop navigator
                getNavigator().setCurrentLocation(null);
                getNavigator().setGoal(null);
                return;
            }

            // Prefer altitude of non-gps sensor
            if (altitude.getRawValue() > 0) {
                location = new Location(location.getLatitude(), location.getLongitude(), altitude.getRawValue());
            }

            MoveVector cmd = getNavigator().update(location);
            if (cmd == null) { // arrived
                move3d(Futures.promise(), 0d, 0d, 0d, 0d); // Cancel any movement

                log.info("Navigator finished at location [{}] for goal [{}]", location, getNavigator().getGoal());
                navigationState.setValue(NavigationState.AVAILABLE);
                navigationStateReason.setValue(NavigationStateReason.FINISHED);
                eventBus.publish(new DroneEventMessage(new NavigationStateChangedMessage(NavigationState.AVAILABLE, NavigationStateReason.FINISHED)));

                getNavigator().setCurrentLocation(null);
                getNavigator().setGoal(null);
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

    private void cancelInternal() {
        synchronized (navigationLock) {
            getNavigator().setGoal(null);
            getNavigator().setCurrentLocation(null);
            setNavigationState(NavigationState.AVAILABLE, NavigationStateReason.STOPPED);
        }
    }

    @Override
    final protected void cancelMoveToLocation(Promise<Void> p) {
        cancelInternal();
        p.success(null);
    }

    @Override
    final protected void moveToLocation(Promise<Void> v, double latitude, double longitude, double altitude) {
        synchronized (navigationLock) {
            if (navigationState.getRawValue() == NavigationState.IN_PROGRESS) {
                v.failure(new DroneException("Already navigating to " + getNavigator().getGoal() + ", abort this first."));
            } else if (navigationState.getRawValue() == NavigationState.UNAVAILABLE) {
                v.failure(new DroneException("Unable to navigate to goal"));
            } else if (!gpsFix.getRawValue()) {
                v.failure(new DroneException("No GPS fix yet."));
            } else {
                getNavigator().setCurrentLocation(location.getRawValue());
                getNavigator().setGoal(new Location(latitude, longitude, altitude));

                setNavigationState(NavigationState.IN_PROGRESS, NavigationStateReason.REQUESTED);
                v.success(null);
            }
        }
    }

    @Override
    protected void landInternal(ActorRef sender, ActorRef self) {
        super.landInternal(sender, self);
        cancelInternal();
    }

    protected abstract LocationNavigator createNavigator(Location currentLocation, Location goal);

    private LocationNavigator getNavigator() {

        if(navigator == null) {
            navigator = createNavigator(null, null);
        }
        return navigator;
    }
}
