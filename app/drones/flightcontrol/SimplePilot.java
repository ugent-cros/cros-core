package drones.flightcontrol;

import akka.actor.ActorRef;
import droneapi.api.DroneCommander;
import droneapi.messages.FlyingStateChangedMessage;
import droneapi.messages.LocationChangedMessage;
import droneapi.messages.NavigationStateChangedMessage;
import droneapi.model.properties.Location;
import droneapi.model.properties.NavigationState;
import drones.flightcontrol.messages.*;
import drones.scheduler.messages.to.FlightCanceledMessage;
import drones.scheduler.messages.to.FlightCompletedMessage;
import models.Checkpoint;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    //List of points(wrapped in messages) where the drone currently is but that need to be evacuated for a landing or take off.
    private List<RequestMessage> evacuationPoints = new ArrayList<>();

    //Range around a no fly point where the drone cannot fly.
    private static final int NO_FY_RANGE = 15;
    //Range around a evacuation point where the drone should be evacuated.
    private static final int EVACUATION_RANGE = 10;

    private boolean landed = true;

    //True if the drone has taken off and is waiting to go up until cruising altitude
    private boolean waitForTakeOffFinished = false;

    //True is the drone is going up until cruising altitude and will wait to fly to the first wayPoint
    private boolean waitForGoUpUntilCruisingAltitudeFinished = false;

    //True if pilot is waiting for landing completed
    private boolean waitForLandFinished = false;

    //True if pilot is waiting for landing  when een stopMessage has send
    private boolean waitForLandAfterStopFinished = false;

    //Buffer when waiting for takeoff or landed to send the completed message
    private RequestMessage requestMessageBuffer;

    private boolean done = false;

    /**
     * @param reporterRef            Actor to report the messages. In theory this should be the same actor that sends the startFlightControlMessage message.
     * @param droneId                  Drone to control.
     * @param linkedWithControlTower True if connected to ControlTower
     * @param wayPoints              Route to fly, the drone will land on the last item
     */
    public SimplePilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints) {
        super(reporterRef, droneId, linkedWithControlTower);

        if (wayPoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.wayPoints = wayPoints;
    }

    public SimplePilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints, double cruisingAltitude) {
        this(reporterRef,droneId,linkedWithControlTower,wayPoints);
        this.cruisingAltitude = cruisingAltitude;
    }

    public SimplePilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints, double cruisingAltitude, List<Location> noFlyPoints) {
        this(reporterRef,droneId,linkedWithControlTower,wayPoints, cruisingAltitude);
        this.cruisingAltitude = cruisingAltitude;
        this.noFlyPoints = new ArrayList<>(noFlyPoints);
    }

    /**
     * Use only for testing!
     */
    public SimplePilot(ActorRef reporterRef, DroneCommander dc, boolean linkedWithControlTower, List<Checkpoint> wayPoints) {
        super(reporterRef, dc, linkedWithControlTower);

        if (wayPoints.isEmpty()) {
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
        if(!landed){
            try {
                Await.ready(dc.land(), MAX_DURATION_LONG);
                landed = true;
            } catch (TimeoutException | InterruptedException e) {
                handleErrorMessage("Could no land drone after stop message");
                return;
            }
            waitForLandAfterStopFinished = true;
        } else {
            stop();
        }

    }

    private void stop(){
        blocked = true;
        dc.unsubscribe(self());
        if(!done || linkedWithControlTower){
            reporterRef.tell(new FlightCanceledMessage(droneId), self());
        }

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
                reporterRef.tell(new RequestMessage(self(),actualLocation, AbstractFlightControlMessage.RequestType.LANDING, droneId),self());
            } else {
                try {
                    Await.ready(dc.land(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no land drone after internal land command");
                    return;
                }
                waitForLandFinished = true;
            }
        }
    }

    private void takeOff() {
        if(!blocked){
            if(linkedWithControlTower){
                reporterRef.tell(new RequestMessage(self(),actualLocation, AbstractFlightControlMessage.RequestType.TAKEOFF, droneId),self());
            } else {
                try {
                    Await.ready(dc.takeOff(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no take off drone after internal takeoff command");
                    return;
                }
                waitForTakeOffFinished = true;
            }
        }
    }

    /**
     * Handles a RequestMessage of a other drone. A RequestMessage is send when a drone wants to land or to take off.
     */
    @Override
    protected void requestMessage(RequestMessage m) {
        if(blocked){
            noFlyPoints.add(m.getLocation());
        } else {
            if (actualLocation.distance(m.getLocation()) <= EVACUATION_RANGE) {
                evacuationPoints.add(m);
            } else {
                noFlyPoints.add(m.getLocation());
                reporterRef.tell(new RequestGrantedMessage(droneId,m), self());
            }
        }
    }

    /**
     * Handles a RequestGrantedMessage. A RequestGrantedMessage is send to a class as a reply on a RequestMessage.
     */
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
                waitForLandFinished = true;
                requestMessageBuffer = m.getRequestMessage();
                break;
            case TAKEOFF:
                try {
                    Await.ready(dc.takeOff(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no take off drone after internal takeoff command");
                    return;
                }
                waitForTakeOffFinished = true;
                requestMessageBuffer = m.getRequestMessage();
                break;
            default:
                log.warning("No handler for: [{}]", m.getRequestMessage().getType());
        }
    }

    /**
     * Handles CompletedMessage of a other drone. A CompletedMessage is send when a other drone has completed his landing of take off that he has requested.
     */
    @Override
    protected void completedMessage(CompletedMessage m) {
        noFlyPoints.remove(m.getLocation());
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        if (!blocked && !waitForLandFinished && !waitForTakeOffFinished && !waitForGoUpUntilCruisingAltitudeFinished) {
            actualLocation = new Location(m.getLatitude(), m.getLongitude(), m.getGpsHeight());
            for (RequestMessage r : evacuationPoints) {
                if (actualLocation.distance(r.getLocation()) > EVACUATION_RANGE) {
                    evacuationPoints.remove(r);
                    noFlyPoints.add(r.getLocation());
                    reporterRef.tell(new RequestGrantedMessage(droneId,r),self());
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
                if(!blocked && waitForTakeOffFinished) {
                    waitForTakeOffFinished = false;
                    //go up until cruising altitude
                    try {
                        Await.ready(dc.moveToLocation(actualLocation.getLatitude(), actualLocation.getLongitude(), cruisingAltitude), MAX_DURATION_LONG);
                    } catch (TimeoutException | InterruptedException e) {
                        handleErrorMessage("Could no send takeoff command  to cruising altitude");
                        return;
                    }
                    waitForGoUpUntilCruisingAltitudeFinished = true;
                }
                break;
            case EMERGENCY:
                handleErrorMessage("Drone in emergency");
                landed = true;
                break;
            case LANDED:
                if(!blocked && waitForLandFinished){
                    waitForLandFinished = false;
                    landed = true;
                    blocked = true;
                    if(linkedWithControlTower){
                        reporterRef.tell(new CompletedMessage(requestMessageBuffer), self());
                    }
                    done = true;
                    reporterRef.tell(new FlightCompletedMessage(droneId, actualLocation), self());
                    return;
                }
                if(!blocked && waitForLandAfterStopFinished){
                    stop();
                    return;
                }
                landed = true;
                blocked = true;
                break;
        }
    }

    @Override
    protected void navigationStateChanged(NavigationStateChangedMessage m) {
        if(!blocked && m.getState() == NavigationState.AVAILABLE){
            switch (m.getReason()){
                case FINISHED:
                    if(waitForGoUpUntilCruisingAltitudeFinished){
                        waitForGoUpUntilCruisingAltitudeFinished = false;
                        landed = false;
                        if(linkedWithControlTower){
                            reporterRef.tell(new CompletedMessage(requestMessageBuffer), self());
                        }
                        goToNextWaypoint();
                        break;
                    }
                    if(!waitForTakeOffFinished && !waitForLandAfterStopFinished && !waitForLandFinished && !waitForGoUpUntilCruisingAltitudeFinished){
                        goToNextWaypoint();
                    }
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
