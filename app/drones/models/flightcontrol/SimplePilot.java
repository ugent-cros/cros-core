package drones.models.flightcontrol;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.dispatch.OnSuccess;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.DroneCommander;
import drones.models.Location;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.DroneArrivalMessage;
import models.Checkpoint;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 *
 * Pilot class to fly with the drone to its destination via the waypoints.
 * He lands on the last item in the list.
 */
public class SimplePilot extends Pilot {

    private Location actualLocation;

    private List<Checkpoint> waypoints;
    private int actualWaypoint = -1;

    //List of points where the drone cannot fly
    private List<Location> noFlyPoints = new ArrayList<>();
    //List of points(wrapped in messages) where the drone currently is but that need to be evacuated for a land.
    private List<LocationMessage> evacuationPoints = new ArrayList<>();

    //Range around a no fly point where the drone cannot fly.
    private static final int NO_FY_RANGE = 4;
    //Range around a evacuation point where the drone should be evacuated.
    private static final int EVACUATION_RANGE = 6;

    
    /**
     * @param reporterRef            Actor to report the messages. In theory this should be the same actor that sends the start message.
     * @param droneId                  Drone to control.
     * @param linkedWithControlTower True if connected to ControlTower
     * @param waypoints Route to fly, the drone will land on the last item
     * @param cruisingAltitude Altitude to fly
     */
    public SimplePilot(ActorRef reporterRef, Long droneId, boolean linkedWithControlTower, List<Checkpoint> waypoints, double cruisingAltitude) {
        this(reporterRef, droneId, linkedWithControlTower, waypoints);
        this.cruisingAltitude = cruisingAltitude;
    }

    public SimplePilot(ActorRef reporterRef, Long droneId, boolean linkedWithControlTower, List<Checkpoint> waypoints) {
        super(reporterRef, droneId, linkedWithControlTower);

        if (waypoints.size() < 1) {
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.waypoints = waypoints;
    }


    /**
     * Use only for testing!
     */
    public SimplePilot(ActorRef reporterRef, DroneCommander dc,boolean linkedWithControlTower, List<Checkpoint> waypoints) {
        super(reporterRef, dc, linkedWithControlTower);

        if (waypoints.size() < 1) {
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.waypoints = waypoints;
    }

    @Override
    public void start() {
        if (cruisingAltitude == 0) {
            cruisingAltitude = DEFAULT_ALTITUDE;
        }
        takeOff();
    }

    @Override
    protected void navigateHomeStateChanged(NavigationStateChangedMessage m) {
        switch (m.getState()) {
            case AVAILABLE:
                switch (m.getReason()) {
                    case ENABLED:
                        goToNextWaypoint();
                        break;
                    case FINISHED:
                        //TO DO wait at checkpoint
                        actualWaypoint++;
                        goToNextWaypoint();
                        break;
                    case STOPPED:
                        //TO DO
                        break;
                }
                break;
            case UNAVAILABLE:
                //TO DO
                break;
            case IN_PROGRESS:
                switch (m.getReason()) {
                    case BATTERY_LOW:
                        //TO DO
                        break;
                    case CONNECTION_LOST:
                        //TO DO
                        break;
                    case REQUESTED:
                        //TO DO
                }
                break;
            case PENDING:
                //TO DO ???
        }
    }

    protected void goToNextWaypoint() {
        if (actualWaypoint >= 0) {
            if (actualWaypoint == waypoints.size()) {
                //arrived at destination => land
                land();
            } else {
                models.Location waypoint = waypoints.get(actualWaypoint).getLocation();
                dc.moveToLocation(waypoint.getLatitude(), waypoint.getLongitude(), cruisingAltitude);
            }
        }
    }

    private void land(){
        if(linkedWithControlTower){
            reporterRef.tell(new RequestMessage(LocationMessage.RequestType.LANDING,self(),actualLocation),self());
        } else {
            dc.land().onSuccess(new OnSuccess<Void>() {

                @Override
                public void onSuccess(Void result) throws Throwable {
                    reporterRef.tell(new DroneArrivalMessage(droneId, actualLocation), self());
                }
            }, getContext().system().dispatcher());
        }
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        actualLocation = new Location(m.getLatitude(),m.getLongitude(),m.getGpsHeigth());
        for(LocationMessage l : evacuationPoints){
            //Check if evacuationPoint is evacuated
            if(actualLocation.distance(l.getLocation()) > EVACUATION_RANGE){
                evacuationPoints.remove(l);
                noFlyPoints.add(l.getLocation());
                reporterRef.tell(new RequestGrantedMessage(l),self());
            }
        }
        for (Location l : noFlyPoints) {
            if (actualLocation.distance(l) < NO_FY_RANGE) {
                //stop with flying
                dc.cancelMoveToLocation();
            }
        }
    }

    @Override
    protected void requestMessage(RequestMessage m) {
        if(actualLocation.distance(m.getLocation()) <= EVACUATION_RANGE){
            evacuationPoints.add(m);
        } else {
            noFlyPoints.add(m.getLocation());
            reporterRef.tell(new RequestGrantedMessage(m),self());
        }
    }

    @Override
    protected void requestGrantedMessage(RequestGrantedMessage m) {
        switch (m.getType()) {
            case LANDING:
                dc.land().onSuccess(new OnSuccess<Void>() {

                    @Override
                    public void onSuccess(Void result) throws Throwable {
                        reporterRef.tell(new CompletedMessage(m), self());
                        reporterRef.tell(new DroneArrivalMessage(droneId, actualLocation), self());
                    }
                }, getContext().system().dispatcher());
                break;
            case TAKEOFF:
                //TO DO on failure
                dc.takeOff().onSuccess(new OnSuccess<Void>() {
                    @Override
                    public void onSuccess(Void result) throws Throwable {
                        reporterRef.tell(new CompletedMessage(m), self());
                        actualWaypoint = 0;
                        goToNextWaypoint();
                    }
                }, getContext().system().dispatcher());
                break;
            default:
                log.warning("No handler for: [{}]", m.getType());
        }
    }

    @Override
    protected void completedMessage(CompletedMessage m) {
        noFlyPoints.remove(m.getLocation());

        //try to fly further
        for (Location l : noFlyPoints) {
            if (actualLocation.distance(l) < NO_FY_RANGE) {
                return;
            }
        }

        //allowed to continue flying
        models.Location waypoint = waypoints.get(actualWaypoint).getLocation();
        dc.moveToLocation(waypoint.getLatitude(), waypoint.getLongitude(), cruisingAltitude);
    }

    private void takeOff(){
        if(linkedWithControlTower){
            reporterRef.tell(new RequestMessage(LocationMessage.RequestType.TAKEOFF,self(),actualLocation),self());
        } else {
            dc.takeOff().onSuccess(new OnSuccess<Void>() {
                @Override
                public void onSuccess(Void result) throws Throwable {
                    actualWaypoint = 0;
                    goToNextWaypoint();
                }
            }, getContext().system().dispatcher());
        }
    }
}
