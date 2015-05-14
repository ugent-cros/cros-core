package drones.flightcontrol;

import akka.actor.ActorRef;
import droneapi.api.DroneCommander;
import droneapi.messages.FlyingStateChangedMessage;
import droneapi.messages.LocationChangedMessage;
import droneapi.messages.NavigationStateChangedMessage;
import droneapi.model.properties.FlyingState;
import droneapi.model.properties.Location;
import droneapi.model.properties.NavigationState;
import drones.flightcontrol.messages.*;
import drones.scheduler.messages.to.FlightCanceledMessage;
import drones.scheduler.messages.to.FlightCompletedMessage;
import models.Checkpoint;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic implementation of a Pilot class. It will fly with the drone to its destinations via the wayPoints
 * and will land on the last item in the list. It takes into account the waiting time of a wayPoint but not
 * its altitude.
 *
 * When the SimplePilot is connected with a ControlTower it will send a request message before a take off or
 * landing. When, subsequently, the RequestGrantedMessage is received it will execute the landing or take off
 * and respond with a CompletedMessage.
 *
 * When a RequestMessage is received from another pilot it will check if its actual location is not within
 * the NoFlyRange of the location of the request. If this is so, it will add the request location to the
 * NoFlyPoint list and it will immediately respond with a RequestGrantedMessage. If this is not so, it will
 * wait until the drone has left the request location.
 *
 * !!! WARNING 1: The SimplePilot assumes that there are no obstacles on the route that he will fly.
 *
 * !!! WARNING 2: When an error occurs, the pilot will go to a blocked state. It is the responsibility of
 * the user to land the drone on a safe place.
 *
 * !!! WARNING 3: There can only be one pilot for each drone at any time.
 *
 * !!! WARNING 4: The drone should be landed before starting the pilot.
 *
 * Created by Sander on 18/03/2015.
 */
public class SimplePilot extends Pilot {

    private Location actualLocation;

    //wayPoints = route to fly
    private List<Checkpoint> wayPoints;
    private int actualWayPoint = -1;

    //List of points(wrapped in messages) where the drone cannot fly
    private List<RequestMessage> noFlyPoints = new ArrayList<>();
    //List of points(wrapped in messages) where the drone currently is but that need to be evacuated for a landing or take off.
    private List<RequestMessage> evacuationPoints = new ArrayList<>();

    //Range around a no fly point where the drone cannot fly.
    private static final int NO_FY_RANGE = 10;
    //Range around a evacuation point where the drone should be evacuated.
    private static final int EVACUATION_RANGE = 15;

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
    private RequestMessage requestMessageBuffer = null;

    //True when entered a no fly range
    private boolean waitForLeavingNoFlyRange = false;

    private boolean done = false;

    /**
     * @param reporterRef            actor to report the outgoing messages
     * @param droneId                drone to control
     * @param linkedWithControlTower true if connected to a ControlTower
     * @param wayPoints              route to fly, the drone will land on the last item
     */
    public SimplePilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints) {
        super(reporterRef, droneId, linkedWithControlTower);

        if (wayPoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.wayPoints = wayPoints;
    }

    /**
     *
     * @param reporterRef               actor to report the outgoing messages
     * @param droneId                   drone to control
     * @param linkedWithControlTower    true if connected to a ControlTower
     * @param wayPoints                 route to fly, the drone will land on the last item
     * @param cruisingAltitude          cruisingAltitude of the drone
     */
    public SimplePilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints, double cruisingAltitude) {
        this(reporterRef, droneId, linkedWithControlTower, wayPoints);
        this.cruisingAltitude = cruisingAltitude;
    }

    /**
     *
     * @param reporterRef               actor to report the outgoing messages
     * @param droneId                   drone to control
     * @param linkedWithControlTower    true if connected to a ControlTower
     * @param wayPoints                 route to fly, the drone will land on the last item
     * @param cruisingAltitude          cruisingAltitude of the drone
     * @param noFlyPoints               list of points where the drone cannot fly
     */
    public SimplePilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower, List<Checkpoint> wayPoints, double cruisingAltitude, List<RequestMessage> noFlyPoints) {
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
        logPilot("has started");
        takeOff();
    }

    @Override
    protected void stopFlightControlMessage(StopFlightControlMessage m) {
        logPilot("has received a shut down message");

        //check if there was a request granted but not yet completed
        if(linkedWithControlTower && requestMessageBuffer != null){
            requestMessageBuffer = null;
            reporterRef.tell(new CompletedMessage(requestMessageBuffer),self());
        }

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
            reporterRef.tell(new FlightCanceledMessage(droneId, done), self());
        }

        logPilot("will shut down");

        //stop
        getContext().stop(self());
    }

    protected void goToNextWaypoint() {
        if (!blocked) {
            actualWayPoint++;
            if (actualWayPoint == 0){
                logPilot("will got to the first way point");
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
                logPilot("has arrived at last way point");
                //arrived at destination => land
                land();
            } else {
                logPilot("has arrived at way point " + (actualWayPoint - 1) + " and will go to the next one");
                //fly to next wayPoint
                models.Location newLocation = wayPoints.get(actualWayPoint).getLocation();
                dc.moveToLocation(newLocation.getLatitude(), newLocation.getLongitude(), cruisingAltitude);
            }
        }
    }

    private void land() {
        if(!blocked){
            if(linkedWithControlTower){
                logPilot("has sent a request for landing");
                reporterRef.tell(new RequestMessage(self(), actualLocation, AbstractFlightControlMessage.RequestType.LANDING, droneId), self());
            } else {
                logPilot("has started the landing procedure");
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
                logPilot("has sent a request for take off");
                reporterRef.tell(new RequestMessage(self(),actualLocation, AbstractFlightControlMessage.RequestType.TAKEOFF, droneId),self());
            } else {
                logPilot("has started the take off procedure");
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
     * Handles a RequestMessage of a other drone. A RequestMessage is sent when a drone wants to land or to take off.
     */
    @Override
    protected void requestMessage(RequestMessage m) {
        if(blocked){
            noFlyPoints.add(m);
            reporterRef.tell(new RequestGrantedMessage(droneId, m), self());
            logPilot("has received a request from " + m.getDroneId() + " and has granted it");
        } else {
            if (actualLocation.distance(m.getLocation()) <= EVACUATION_RANGE) {
                evacuationPoints.add(m);
                logPilot("has received a request from " + m.getDroneId() + " and has added it to the evacuation points");
            } else {
                noFlyPoints.add(m);
                reporterRef.tell(new RequestGrantedMessage(droneId, m), self());
                logPilot("has received a request from " + m.getDroneId() + " and has granted it");
            }
        }
    }

    /**
     * Handles a RequestGrantedMessage. A RequestGrantedMessage is sent to a class as a reply on a RequestMessage.
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
                if(linkedWithControlTower){
                    requestMessageBuffer = m.getRequestMessage();
                }
                logPilot("has received a RequestGrantedMessage and has started the landing procedure");
                break;
            case TAKEOFF:
                try {
                    Await.ready(dc.takeOff(), MAX_DURATION_LONG);
                } catch (TimeoutException | InterruptedException e) {
                    handleErrorMessage("Could no take off drone after internal takeoff command");
                    return;
                }
                waitForTakeOffFinished = true;
                if(linkedWithControlTower){
                    requestMessageBuffer = m.getRequestMessage();
                }
                logPilot("has received a RequestGrantedMessage and has started the take off procedure");
                break;
            default:
                log.warning("No handler for: [{}]", m.getRequestMessage().getType());
        }
    }

    /**
     * Handles CompletedMessage of a other drone. A CompletedMessage is sent when a other drone has completed his landing of take off that he has requested.
     */
    @Override
    protected void completedMessage(CompletedMessage m) {
        logPilot("has received a CompletedMessage");
        noFlyPoints.remove(m.getRequestMessage());
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        if (!blocked && !waitForLandFinished && !waitForTakeOffFinished && !waitForGoUpUntilCruisingAltitudeFinished) {
            actualLocation = new Location(m.getLatitude(), m.getLongitude(), m.getGpsHeight());
            //use iterator
            Iterator<RequestMessage> it = evacuationPoints.iterator();
            while(it.hasNext()){
                RequestMessage r = it.next();
                if(actualLocation.distance(r.getLocation()) > EVACUATION_RANGE){
                    logPilot("has left the evacuation range");
                    //remove from list
                    it.remove();
                    noFlyPoints.add(r);
                    reporterRef.tell(new RequestGrantedMessage(droneId,r),self());
                }
            }
            for (RequestMessage requestMessage : noFlyPoints) {
                Location l = requestMessage.getLocation();
                if (actualLocation.distance(l) < NO_FY_RANGE && !landed) {
                    logPilot("has entered a no fly range");
                    //stop with flying
                    waitForLeavingNoFlyRange = true;
                    try {
                        Await.ready(dc.cancelMoveToLocation(), MAX_DURATION_SHORT);
                    } catch (TimeoutException | InterruptedException e) {
                        handleErrorMessage("Cannot cancelMoveToLocation, the drones will probably collide!!!");
                    }
                    return;
                }
            }
            //Check if can fly further
            if(waitForLeavingNoFlyRange){
                waitForLeavingNoFlyRange = false;
                //fly to next wayPoint
                models.Location newLocation = wayPoints.get(actualWayPoint).getLocation();
                dc.moveToLocation(newLocation.getLatitude(), newLocation.getLongitude(), cruisingAltitude);
                logPilot("can no fly further to the next way point: " + actualWayPoint);
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
                    logPilot("has completed the first take off procedure and will now go up until cruising altitude");
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
                        requestMessageBuffer = null;
                    }
                    done = true;
                    reporterRef.tell(new FlightCompletedMessage(droneId, actualLocation), self());
                    logPilot("has completed the landing procedure");
                    return;
                }
                if(!blocked && waitForLandAfterStopFinished){
                    logPilot("has completed the landing procedure");
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
                            requestMessageBuffer = null;
                        }
                        logPilot("has completed the second take off procedure");
                        goToNextWaypoint();
                        break;
                    }
                    if(!waitForTakeOffFinished && !waitForLandAfterStopFinished && !waitForLandFinished && !waitForGoUpUntilCruisingAltitudeFinished){
                        goToNextWaypoint();
                    }
                    break;
                case STOPPED:
                    if(!linkedWithControlTower){
                        handleErrorMessage("Navigation has stopped.");
                    }
            }

        }
    }

    @Override
    protected void addNoFlyPointMessage(AddNoFlyPointMessage m) {
        if (actualLocation.distance(m.getNoFlyPoint().getLocation()) < NO_FY_RANGE) {
            handleErrorMessage("You cannot add a drone within the no-fly-range " +
                    "of the location where another drone wants to land or to take off");
        } else {
            noFlyPoints.add(m.getNoFlyPoint());
        }
    }

    private void handleErrorMessage(String s){
        blocked = true;
        reporterRef.tell(new FlightControlExceptionMessage(s,droneId),self());
        log.error("FlightControl error with droneID " + droneId + ": " + s);
    }

    private void logPilot(String s){
        log.info("Pilot for drone {} {}.", droneId, s);
    }
}
