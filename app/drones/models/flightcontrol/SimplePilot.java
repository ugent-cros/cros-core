package drones.models.flightcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import akka.actor.ActorRef;
import drones.messages.FlyingStateChangedMessage;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.*;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.messages.to.FlightCompletedMessage;
import drones.models.scheduler.messages.to.FlightCanceledMessage;
import models.Checkpoint;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 * Created by Sander on 18/03/2015.
 *
 * Pilot class to fly with the drone to its destination via the wayPoints.
 * He lands on the last item in the list.
 */
public class SimplePilot extends Pilot {

    private Location actualLocation;

    private List<Checkpoint> wayPoints;
    private int actualWayPoint = -1;

    //List of points where the drone cannot fly
    private List<Location> noFlyPoints = new ArrayList<>();
    //List of points(wrapped in messages) where the drone currently is but that need to be evacuated for a land.
    private List<RequestMessage> evacuationPoints = new ArrayList<>();

    //Range around a no fly point where the drone cannot fly.
    private static final int NO_FY_RANGE = 15;
    //Range around a evacuation point where the drone should be evacuated.
    private static final int EVACUATION_RANGE = 10;

    private boolean landed = true;

    //True if the drone has taken off and is waiting to fly to the first wayPoint
    private boolean waitForFirstWayPoint = false;
    
    /**
     * @param reporterRef            Actor to report the messages. In theory this should be the same actor that sends the startFlightControlMessage message.
     * @param droneId                  Drone to control.
     * @param linkedWithControlTower True if connected to ControlTower
     * @param wayPoints              Route to fly, the drone will land on the last item
     */
    public SimplePilot(ActorRef reporterRef, Long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints) {
        super(reporterRef, droneId, linkedWithControlTower);

        if (wayPoints.size() < 1) {
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.wayPoints = wayPoints;
    }

    /**
     * Use only for testing!
     */
    public SimplePilot(ActorRef reporterRef, DroneCommander dc, boolean linkedWithControlTower, List<Checkpoint> wayPoints) {
        super(reporterRef, dc, linkedWithControlTower);

        if (wayPoints.size() < 1) {
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.wayPoints = wayPoints;
    }

    @Override
    public void startFlightControlMessage() {
        //Check if navigationState is "AVAILABLE"
        try {
            NavigationState m = Await.result(dc.getNavigationState(), MAX_DURATION_SHORT);
            if(m != NavigationState.AVAILABLE){
                handleErrorMessage("Can not start because NavigationState is not \"AVAILABLE\".");
                return;
            }
            actualLocation = Await.result(dc.getLocation(), MAX_DURATION_SHORT);
        } catch (Exception e) {
            handleErrorMessage("Error while getting NavigationState after start");
            return;
        }

        if (Double.doubleToRawLongBits(cruisingAltitude) == 0) {
            cruisingAltitude = DEFAULT_ALTITUDE;
            try {
                Await.ready(dc.setMaxHeight((float) cruisingAltitude), MAX_DURATION_SHORT);
            } catch (TimeoutException | InterruptedException e) {
                handleErrorMessage("Failed to set max height after SetCruisingAltitudeMessage");
                return;
            }
        }
        blocked = false;
        takeOff();
    }

    @Override
    protected void stopFlightControlMessage(StopFlightControlMessage m) {
        if(!blocked && !landed){
            try {
                Await.ready(dc.land(), MAX_DURATION_LONG);
            } catch (TimeoutException | InterruptedException e) {
                handleErrorMessage("Could no land drone after stop message");
                return;
            }
        }
        blocked = true;
        reporterRef.tell(new FlightCanceledMessage(droneId), self());
        //stop
        getContext().stop(self());
    }

    protected void goToNextWaypoint() {
        if (!blocked) {
            actualWayPoint++;
            if (actualWayPoint == 0){
                models.Location newLocation = wayPoints.get(actualWayPoint).getLocation();
                dc.moveToLocation(newLocation.getLatitude(), newLocation.getLongitude(), cruisingAltitude);
            } else {
                //wait at wayPoint
                getContext().system().scheduler().scheduleOnce(Duration.create(wayPoints.get(actualWayPoint - 1).getWaitingTime(), TimeUnit.SECONDS),
                        new Runnable() {
                            @Override
                            public void run() {
                                self().tell(new WaitAtWayPointCompletedMessage(),self());                                    }
                        }, getContext().system().dispatcher());
            }
        }

    }

    @Override
    protected void waitAtWayPointCompletedMessage(WaitAtWayPointCompletedMessage m) {
        if(!blocked){
            reporterRef.tell(new WayPointCompletedMessage(droneId, actualWayPoint -1), self());
            if (actualWayPoint == wayPoints.size()) {
                //arrived at destination => land
                land();
            } else {
                //fly to next wayPoint
                models.Location newLocation = wayPoints.get(actualWayPoint).getLocation();
                dc.moveToLocation(newLocation.getLatitude(), newLocation.getLongitude(), cruisingAltitude);
            }
        }
    }

    private void land() {
        if(!blocked){
            if(linkedWithControlTower){
                reporterRef.tell(new RequestMessage(self(),actualLocation, AbstractFlightControlMessage.RequestType.LANDING),self());
            } else {
                try {
                    Await.ready(dc.land(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no land drone after internal land command");
                    return;
                }
                blocked = true;
                landed = true;
                reporterRef.tell(new FlightCompletedMessage(droneId,actualLocation),self());
            }
        }
    }

    private void takeOff() {
        if(!blocked){
            if(linkedWithControlTower){
                reporterRef.tell(new RequestMessage(self(),actualLocation, AbstractFlightControlMessage.RequestType.TAKEOFF),self());
            } else {
                try {
                    Await.ready(dc.takeOff(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no take off drone after internal takeoff command");
                    return;
                }
                landed = false;
                waitForFirstWayPoint = true;
            }
        }
    }

    @Override
    protected void requestMessage(RequestMessage m) {
        if(blocked){
            noFlyPoints.add(m.getLocation());
        } else {
            if (actualLocation.distance(m.getLocation()) <= EVACUATION_RANGE) {
                evacuationPoints.add(m);
            } else {
                noFlyPoints.add(m.getLocation());
                reporterRef.tell(new RequestGrantedMessage(m), self());
            }
        }
    }

    @Override
    protected void requestGrantedMessage(RequestGrantedMessage m) {
        switch (m.getRequestMessage().getType()) {
            case LANDING:
                try {
                    Await.ready(dc.land(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no land drone after internal land command");
                    return;
                }
                blocked = true;
                landed = true;
                reporterRef.tell(new CompletedMessage(m.getRequestMessage()), self());
                reporterRef.tell(new FlightCompletedMessage(droneId, actualLocation), self());
                break;
            case TAKEOFF:
                try {
                    Await.ready(dc.takeOff(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no take off drone after internal takeoff command");
                    return;
                }
                landed = false;
                //go up until cruising altitude
                try {
                    Await.ready(dc.moveToLocation(actualLocation.getLatitude(), actualLocation.getLongitude(), cruisingAltitude), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no take off to cruising altitude after internal takeoff command");
                    return;
                }
                waitForFirstWayPoint = true;
                reporterRef.tell(new CompletedMessage(m.getRequestMessage()), self());
                break;
            default:
                log.warning("No handler for: [{}]", m.getRequestMessage().getType());
        }
    }

    @Override
    protected void completedMessage(CompletedMessage m) {
        noFlyPoints.remove(m.getLocation());
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        if (!blocked) {
            actualLocation = new Location(m.getLatitude(), m.getLongitude(), m.getGpsHeight());
            for (RequestMessage r : evacuationPoints) {
                if (actualLocation.distance(r.getLocation()) > EVACUATION_RANGE) {
                    evacuationPoints.remove(r);
                    noFlyPoints.add(r.getLocation());
                    reporterRef.tell(new RequestGrantedMessage(r),self());
                }
            }
            for (Location l : noFlyPoints) {
                if (actualLocation.distance(l) < NO_FY_RANGE) {
                    //stop with flying
                    try {
                        Await.ready(dc.cancelMoveToLocation(), MAX_DURATION_SHORT);
                    } catch (TimeoutException | InterruptedException e) {
                        handleErrorMessage("Cannot cancelMoveToLocation, the drones will probably collide!!!");
                    }
                }
            }
        }

    }

    @Override
    protected void flyingStateChanged(FlyingStateChangedMessage m) {
        switch (m.getState()){
            case HOVERING:
                if(!blocked && waitForFirstWayPoint){
                    waitForFirstWayPoint = false;
                    goToNextWaypoint();
                }
                break;
            case EMERGENCY:
                handleErrorMessage("Drone in emergency");
                break;
            case LANDED:
                blocked = true;
                break;
        }
    }

    @Override
    protected void navigationStateChanged(NavigationStateChangedMessage m) {
        if(!blocked && m.getState() == NavigationState.AVAILABLE){
            switch (m.getReason()){
                case FINISHED:
                    goToNextWaypoint();
                    break;
                case STOPPED:
                    handleErrorMessage("Navigation has stopped.");
            }

        }
    }

    @Override
    protected void addNoFlyPointMessage(AddNoFlyPointMessage m) {
        if (actualLocation.distance(m.getNoFlyPoint()) < NO_FY_RANGE) {
            handleErrorMessage("You cannot add a drone within the no-fly-range " +
                    "of the location where another drone wants to land or to take off");
        } else {
            noFlyPoints.add(m.getNoFlyPoint());
        }
    }
}
