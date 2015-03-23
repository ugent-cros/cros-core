package drones.models.flightcontrol;

import java.util.ArrayList;
import java.util.List;

import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.Location;
import models.Checkpoint;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 * 
 * Pilot class to fly with the drone to its destination via the waypoints.
 * He lands on the last item in the list.
 */
public class SimplePilot extends Pilot{

	private List<Checkpoint> waypoints;
    private int  actualWaypoint = -1;

    //List of points where the drone cannot fly
    private List<Location> noFlyPoints = new ArrayList<>();
    //List of points where the drone currently is but that need to be evacuated.
    private List<Location> evacuationPoints = new ArrayList<>();

    private static final int NO_FY_RANGE = 3;
    private static final int AVACUATION_RANGE = 5;

    public SimplePilot(Drone drone, List<Checkpoint> waypoints) {
        super(drone);

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
                //TO DO
                break;
            case PENDING:
                //TO Do
        }
    }

    protected void goToNextWaypoint(){
        if(actualWaypoint >= 0){
            if(actualWaypoint == waypoints.size()){
                //TO DO tell done
            } else {
                models.Location waypoint = waypoints.get(actualWaypoint).getLocation();
                dc.moveToLocation(waypoint.getLatitude(),waypoint.getLongitude(),altitude);
            }
        }
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        
    }
}
