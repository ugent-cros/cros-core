import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import api.DroneCommander;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.messages.to.FlightCanceledMessage;
import drones.models.scheduler.messages.to.FlightCompletedMessage;
import model.properties.FlyingState;
import model.properties.Location;
import models.Checkpoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import simulator.BepopSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

/**
 * Created by Sander on 5/04/2015.
 */
public class SimplePilotTest extends TestSuperclass {

    private static final Location STERRE = new Location(51.0226, 3.71, 0);
    private static final double MAX_HEIGHT = 100;
    private static final double ANGLE_WRT_EQUATOR = 0;
    private static final double TOP_SPEED = 50;

    public static final FiniteDuration MAX_DURATION_MESSAGE = Duration.create(10, "seconds");
    public static final FiniteDuration MAX_DURATION_FLYING = Duration.create(120, "seconds");

    private static ActorSystem system;
    private List<Checkpoint> wayPoints;
    private Checkpoint destination;

    public SimplePilotTest() {
        wayPoints = new ArrayList<>();
        wayPoints.add(new Checkpoint(51.0226, 3.72, 0));
        destination = new Checkpoint(51.0226, 3.73, 0);
        wayPoints.add(destination);
    }

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        system = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
    }

    /**
     * Check if SimplePilot flies to the destination
     * @throws TimeoutException
     * @throws InterruptedException
     */
    @Test
    public void normalFlow() throws TimeoutException, InterruptedException {
        new JavaTestKit(system) {
            {
                //init
                final ActorRef bebopSimulator = system.actorOf(
                        Props.create(BepopSimulator.class,
                                () -> new BepopSimulator(STERRE, MAX_HEIGHT, ANGLE_WRT_EQUATOR, TOP_SPEED)));
                final DroneCommander dc = new DroneCommander(bebopSimulator);
                Await.ready(dc.init(), MAX_DURATION_MESSAGE);
                final ActorRef simplePilot = system.actorOf(
                        Props.create(SimplePilot.class,
                                () -> new SimplePilot(getRef(), dc, false, wayPoints))
                );

                simplePilot.tell(new StartFlightControlMessage(), getRef());

                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, FlightCompletedMessage.class);

                //check if on destination
                Location droneLocation;
                try {
                    droneLocation = Await.result(dc.getLocation(), MAX_DURATION_MESSAGE);
                    double d = droneLocation.distance(destination.getLocation().getLongitude(), destination.getLocation().getLatitude());
                    assertTrue("Check dronelocation: " + d,d < 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //check if landed
                try {
                    FlyingState flyingState = Await.result(dc.getFlyingState(), MAX_DURATION_MESSAGE);
                    assertTrue("Check drone status",flyingState == FlyingState.LANDED);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                simplePilot.tell(new StopFlightControlMessage(),getRef());

                expectMsgClass(MAX_DURATION_MESSAGE, FlightCanceledMessage.class);
            }
        };
    }

    /**
     * Test if correct request messages are used for landing/takeoff.
     *
     */
    @Test
    public void requestMessages() throws TimeoutException, InterruptedException {
        new JavaTestKit(system) {
            {
                //init
                final ActorRef bebopSimulator = system.actorOf(
                        Props.create(BepopSimulator.class,
                                () -> new BepopSimulator(STERRE, MAX_HEIGHT, ANGLE_WRT_EQUATOR, TOP_SPEED)));
                final DroneCommander dc = new DroneCommander(bebopSimulator);
                Await.ready(dc.init(), MAX_DURATION_MESSAGE);
                final ActorRef simplePilot = system.actorOf(
                        Props.create(SimplePilot.class,
                                () -> new SimplePilot(getRef(), dc, true, wayPoints))
                );

                //start
                simplePilot.tell(new StartFlightControlMessage(), getRef());

                //request for take off
                expectMsgClass(MAX_DURATION_FLYING, RequestMessage.class);

                simplePilot.tell(new RequestGrantedMessage(null,new RequestMessage(simplePilot,STERRE,AbstractFlightControlMessage.RequestType.TAKEOFF,null)),getRef());

                expectMsgClass(MAX_DURATION_FLYING, CompletedMessage.class);

                //at wayPoints
                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);

                //request for landing
                expectMsgClass(MAX_DURATION_FLYING, RequestMessage.class);

                Location tmp = new Location(destination.getLocation().getLatitude(),destination.getLocation().getLongitude(),destination.getLocation().getAltitude());
                simplePilot.tell(new RequestGrantedMessage(null,new RequestMessage(simplePilot,tmp,AbstractFlightControlMessage.RequestType.LANDING, null)),getRef());

                expectMsgAnyClassOf(MAX_DURATION_FLYING,FlightCompletedMessage.class,CompletedMessage.class);
                expectMsgAnyClassOf(MAX_DURATION_FLYING,FlightCompletedMessage.class,CompletedMessage.class);

                simplePilot.tell(new StopFlightControlMessage(),getRef());

                expectMsgClass(MAX_DURATION_MESSAGE, FlightCanceledMessage.class);
            }
        };
    }
}