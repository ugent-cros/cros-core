package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.dispatch.OnSuccess;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.FlyingStateChangedMessage;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.*;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.FlightControlExceptionMessage;
import drones.models.scheduler.messages.DroneArrivalMessage;
import models.Checkpoint;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by Sander on 18/03/2015.
 * <p>
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
    private List<RequestMessage> evacuationPoints = new ArrayList<>();

    //Range around a no fly point where the drone cannot fly.
    private static final int NO_FY_RANGE = 4;
    //Range around a evacuation point where the drone should be evacuated.
    private static final int EVACUATION_RANGE = 6;


    private boolean landed = true;
    private boolean waitForTakeOffDone = false;
    private boolean waitForCancelLandedCompleted = false;


    /**
     * @param reporterRef            Actor to report the messages. In theory this should be the same actor that sends the start message.
     * @param droneId                Drone to control.
     * @param linkedWithControlTower True if connected to ControlTower
     * @param waypoints              Route to fly, the drone will land on the last item
     * @param cruisingAltitude       Altitude to fly
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
    public SimplePilot(ActorRef reporterRef, DroneCommander dc, boolean linkedWithControlTower, List<Checkpoint> waypoints) {
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
            try {
                Await.ready(dc.setMaxHeight((float) cruisingAltitude), MAX_DURATION_MESSAGE);
            } catch (TimeoutException | InterruptedException e) {
                e.printStackTrace();
                reporterRef.tell(new DroneException("Failed to set max height before starting"),self());
            }
        }
        takeOff();
    }

    @Override
    protected void navigationStateChanged(NavigationStateChangedMessage m) {
        if(m.getState() == NavigationState.AVAILABLE){
            //TO DO wait at checkpoint
            actualWaypoint++;
            goToNextWaypoint();
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

    private void land() {
        if (linkedWithControlTower) {
            reporterRef.tell(new RequestMessage(LocationMessage.RequestType.LANDING, self(), actualLocation), self());
        } else {
            dc.land().onSuccess(new OnSuccess<Void>() {

                @Override
                public void onSuccess(Void result) throws Throwable {
                    landed = true;
                    reporterRef.tell(new DroneArrivalMessage(droneId, actualLocation), self());
                }
            }, getContext().system().dispatcher());
        }
    }

    @Override
    protected void locationChanged(LocationChangedMessage m) {
        actualLocation = new Location(m.getLatitude(), m.getLongitude(), m.getGpsHeight());
        for (RequestMessage l : evacuationPoints) {
            //Check if evacuationPoint is evacuated
            if (actualLocation.distance(l.getLocation()) > EVACUATION_RANGE) {
                evacuationPoints.remove(l);
                noFlyPoints.add(l.getLocation());
                reporterRef.tell(new RequestGrantedMessage(l), self());
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
        if (actualLocation.distance(m.getLocation()) <= EVACUATION_RANGE) {
            evacuationPoints.add(m);
        } else {
            noFlyPoints.add(m.getLocation());
            reporterRef.tell(new RequestGrantedMessage(m), self());
        }
    }

    @Override
    protected void requestGrantedMessage(RequestGrantedMessage m) {
        switch (m.getRequestMessage().getType()) {
            case LANDING:
                dc.land().onSuccess(new OnSuccess<Void>() {

                    @Override
                    public void onSuccess(Void result) throws Throwable {
                        landed = true;
                        reporterRef.tell(new CompletedMessage(m.getRequestMessage()), self());
                        if(waitForCancelLandedCompleted){
                            reporterRef.tell(new CancelControlCompletedMessage(droneId), self());
                        } else {
                            reporterRef.tell(new DroneArrivalMessage(droneId, actualLocation), self());
                        }
                    }
                }, getContext().system().dispatcher());
                break;
            case TAKEOFF:
                //TO DO on failure
                dc.takeOff().onSuccess(new OnSuccess<Void>() {
                    @Override
                    public void onSuccess(Void result) throws Throwable {
                        landed = false;
                        reporterRef.tell(new CompletedMessage(m.getRequestMessage()), self());
                        actualWaypoint = 0;
                        goToNextWaypoint();
                    }
                }, getContext().system().dispatcher());
                break;
            default:
                log.warning("No handler for: [{}]", m.getRequestMessage().getType());
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

    private void takeOff() {
        if (linkedWithControlTower) {
            reporterRef.tell(new RequestMessage(LocationMessage.RequestType.TAKEOFF, self(), actualLocation), self());
        } else {
            dc.takeOff().onSuccess(new OnSuccess<Void>() {
                @Override
                public void onSuccess(Void result) throws Throwable {
                    landed = false;
                    actualWaypoint = 0;
                    goToNextWaypoint();
                }
            }, getContext().system().dispatcher());
        }
    }

    private void addNoFlyPointMessage(AddNoFlyPointMessage m) {
        if (actualLocation.distance(m.getNoFlyPoint()) < NO_FY_RANGE) {
            reporterRef.tell(new FlightControlExceptionMessage("You cannot add a drone within the no-fly-range " +
                    "of the location where another drone wants to land or to take off"), self());
        } else {
            noFlyPoints.add(m.getNoFlyPoint());
        }
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return super.createListeners().match(AddNoFlyPointMessage.class,
                (m) -> addNoFlyPointMessage(m));
    }

    @Override
    protected void shutDownMessage(ShutDownMessage m) {
        if (landed) {
            self().tell(PoisonPill.getInstance(), sender());
        } else {
            reporterRef.tell(new FlightControlExceptionMessage("Pilot must be canceled first."), self());
        }
    }

    @Override
    protected void emergencyLandingMessage(EmergencyLandingMessage m) {
        try {
            Await.ready(dc.land(), Duration.create(120, "seconds"));
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
            reporterRef.tell(new FlightControlExceptionMessage("Drone does not react within 120 seconds"), self());
        }
        waitForTakeOffDone = true;
    }

    @Override
    protected void flyingStateChanged(FlyingStateChangedMessage m) {
        if(waitForTakeOffDone && m.getState() == FlyingState.HOVERING){
            waitForTakeOffDone = false;
            actualWaypoint++;
            goToNextWaypoint();
        }
    }

    @Override
    protected void cancelControlMessage(CancelControlMessage m) {
        try {
            Await.ready(dc.cancelMoveToLocation(), MAX_DURATION_MESSAGE);
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
            reporterRef.tell(new FlightControlExceptionMessage("Unable to cancel move to location."), self());
            return;
        }
        waitForCancelLandedCompleted = true;
        land();
    }
}
