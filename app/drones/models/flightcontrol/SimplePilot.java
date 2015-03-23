package drones.models.flightcontrol;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.dispatch.OnSuccess;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.Location;
import drones.models.scheduler.DroneArrivalMessage;
import models.Checkpoint;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 * 
 * Pilot class to fly with the drone to its destination via the waypoints.
 * He lands on the last item in the list.
 */
public class SimplePilot extends Pilot{

    private Location actualLocation;

	private List<Checkpoint> waypoints;
    private int  actualWaypoint = -1;

    //List of points where the drone cannot fly
    private List<Location> noFlyPoints = new ArrayList<>();
    //List of points where the drone currently is but that need to be evacuated.
    private List<Location> evacuationPoints = new ArrayList<>();

    private static final int NO_FY_RANGE = 4;
    private static final int EVACUATION_RANGE = 6;

    public SimplePilot(ActorRef actorRef, Drone drone,boolean withControlTower, List<Checkpoint> waypoints) {
        super(actorRef, drone, withControlTower);

        if(waypoints.size() < 1){
            throw new IllegalArgumentException("Waypoints must contain at least 1 element");
        }
        this.waypoints = waypoints;
    }

    @Override
    public void start() {
        if (altitude == 0) {
            altitude = DEFAULT_ALTITUDE;
        }
        actualWaypoint = 0;
    }

    @Override
    protected void navigateHomeStateChanged(NavigationStateChangedMessage m) {
        switch (m.getState()){
            case AVAILABLE:
                switch (m.getReason()){
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
                switch (m.getReason()){
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

    protected void goToNextWaypoint(){
        if(actualWaypoint >= 0){
            if(actualWaypoint == waypoints.size()){
                //arrived at destination => land
                land();
            } else {
                models.Location waypoint = waypoints.get(actualWaypoint).getLocation();
                dc.moveToLocation(waypoint.getLatitude(),waypoint.getLongitude(),altitude);
            }
        }
    }

    private void land(){
        if(linkedWithControlTower){
            actorRef.tell(new RequestForLandingAckMessage(actualLocation),self());
        } else {
            dc.land().onSuccess(new OnSuccess<Void>(){

                @Override
                public void onSuccess(Void result) throws Throwable {
                    actorRef.tell(new DroneArrivalMessage(drone, actualLocation),self());
                }
            }, getContext().system().dispatcher());
        }
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        actualLocation = new Location(m.getLatitude(),m.getLongitude(),m.getGpsHeigth());
        for(Location l : evacuationPoints){
            if(actualLocation.distance(l) > EVACUATION_RANGE){
                evacuationPoints.remove(l);
                noFlyPoints.add(l);
                actorRef.tell(new RequestForLandingAckMessage(l), self());
            }
        }
        for(Location l : noFlyPoints){
            if(actualLocation.distance(l) < NO_FY_RANGE){
                //stop with flying
                dc.cancelMoveToLocation();
            }
        }
    }

    @Override
    protected void requestForLandingMessage(RequestForLandingMessage m) {
        if(actualLocation.distance(m.getLocation()) <= EVACUATION_RANGE){
            evacuationPoints.add(m.getLocation());
        } else {
            noFlyPoints.add(m.getLocation());
            actorRef.tell(new RequestForLandingAckMessage(m.getLocation()), self());
        }
    }

    @Override
    protected void requestForLandingAckMessage(RequestForLandingAckMessage m) {
        dc.land().onSuccess(new OnSuccess<Void>(){

            @Override
            public void onSuccess(Void result) throws Throwable {
                actorRef.tell(new DroneArrivalMessage(drone, actualLocation),self());
            }
        }, getContext().system().dispatcher());
    }

    @Override
    protected void landingCompletedMessage(LandingCompletedMessage m) {
        noFlyPoints.remove(m.getLocation());

        //try to fly further
        for(Location l : noFlyPoints){
            if(actualLocation.distance(l) < NO_FY_RANGE){
                return;
            }
        }

        //allowed to continue flying
        models.Location waypoint = waypoints.get(actualWaypoint).getLocation();
        dc.moveToLocation(waypoint.getLatitude(),waypoint.getLongitude(),altitude);
    }
}
