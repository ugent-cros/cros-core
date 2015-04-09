import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.util.Timeout;
import drones.models.DroneCommander;
import drones.models.Location;
import drones.models.flightcontrol.SimplePilot;
import drones.models.flightcontrol.StartFlightControlMessage;
import drones.models.scheduler.DroneArrivalMessage;
import drones.simulation.BepopSimulator;
import models.Checkpoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import scala.*;
import scala.Long;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by Sander on 5/04/2015.
 */
public class SimplePilotTest extends TestSuperclass{

    private static final Location STERRE = new Location(51.0226,3.71, 0);
    private static final double MAX_HEIGHT = 100;
    private static final double ANGLE_WRT_EQUATOR = 0;
    private static final double TOP_SPEED = 50;


    static ActorSystem system;


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

    @Test
    public void normalFlow() throws TimeoutException, InterruptedException {
        new JavaTestKit(system) {{
            //init
            final ActorRef bebopSimulator = system.actorOf(
                    Props.create(BepopSimulator.class,
                            () -> new BepopSimulator(STERRE, MAX_HEIGHT, ANGLE_WRT_EQUATOR, TOP_SPEED)));
            final DroneCommander dc = new DroneCommander(bebopSimulator);
            Await.ready(dc.init(),Duration.create(10, "seconds"));
            List<Checkpoint> wayPoints = new ArrayList<>();
            wayPoints.add(new Checkpoint(3.72, 51.0226, 0));
            final ActorRef simplePilot = system.actorOf(
                    Props.create(SimplePilot.class,
                            () -> new SimplePilot(getRef(),dc,false,wayPoints))
            );

            simplePilot.tell(new StartFlightControlMessage(),getRef());

            expectMsgClass(duration("120 second"),DroneArrivalMessage.class);
        }
        };
    }
}
