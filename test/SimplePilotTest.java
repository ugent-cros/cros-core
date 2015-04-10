import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import drones.models.DroneCommander;
import drones.models.FlyingState;
import drones.models.Location;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.messages.DroneArrivalMessage;
import drones.simulation.BepopSimulator;
import models.Checkpoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
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
        wayPoints.add(new Checkpoint(3.72, 51.0226, 0));
        destination = new Checkpoint(3.73, 51.0226, 0);
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

                expectMsgClass(MAX_DURATION_FLYING, DroneArrivalMessage.class);

                //check if on destination
                Location droneLocation;
                try {
                    droneLocation = Await.result(dc.getLocation(), MAX_DURATION_MESSAGE);
                    double d = droneLocation.distance(destination.getLocation().getLongitude(), destination.getLocation().getLatitude());
                    assertTrue(d < 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //check if landed
                try {
                    FlyingState flyingState = Await.result(dc.getFlyingState(), MAX_DURATION_MESSAGE);
                    assertTrue(flyingState == FlyingState.LANDED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * Test if correct request messages are used for landing/takeoff.
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

                simplePilot.tell(new StartFlightControlMessage(), getRef());

                expectMsgClass(MAX_DURATION_FLYING, RequestForTakeOffMessage.class);

                simplePilot.tell(new RequestForTakeOffGrantedMessage(simplePilot,STERRE),getRef());

                expectMsgClass(MAX_DURATION_FLYING, TakeOffCompletedMessage.class);

                expectMsgClass(MAX_DURATION_FLYING, RequestForLandingMessage.class);

                Location tmp = new Location(destination.getLocation().getLatitude(),destination.getLocation().getLongitude(),destination.getLocation().getAltitude());
                simplePilot.tell(new RequestForLandingGrantedMessage(simplePilot,tmp),getRef());

                expectMsgAnyClassOf(MAX_DURATION_FLYING,DroneArrivalMessage.class,LandingCompletedMessage.class);
                expectMsgAnyClassOf(MAX_DURATION_FLYING,DroneArrivalMessage.class,LandingCompletedMessage.class);
            }
        };
    }
}
