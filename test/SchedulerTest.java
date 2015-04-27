import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.DroneCommander;
import drones.models.DroneException;
import drones.models.Fleet;
import drones.models.FlyingState;
import drones.models.flightcontrol.messages.StartFlightControlMessage;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.SchedulerException;
import drones.models.scheduler.SimpleScheduler;
import drones.models.scheduler.messages.to.AssignmentMessage;
import drones.models.scheduler.messages.to.EmergencyMessage;
import drones.simulation.SimulatorDriver;
import models.Assignment;
import models.Checkpoint;
import models.Drone;
import models.DroneType;
import org.junit.*;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 6/04/2015.
 */
public class SchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    private static final Duration timeout = Duration.create(3000,TimeUnit.MILLISECONDS);

    @BeforeClass
    public static void createSystem() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void destroySystem() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Override
    public void before() {
        startFakeApplication();
        super.before();
    }

    @After
    public void after() {
        stopFakeApplication();
    }

    @Test
    public void getScheduler_Started_Succeeds() throws Exception{
        Scheduler.getScheduler();
    }

    @Test
    public void stop_Started_Succeeds() throws Exception{
        Scheduler.stop();
    }

    @Test(expected = SchedulerException.class)
    public void start_Started_Fails() throws Exception{
        Scheduler.start(SimpleScheduler.class);
    }

    @Test
    public void start_NotStarted_Succeeds() throws Exception{
        Scheduler.stop();
        Scheduler.start(SimpleScheduler.class);
    }

    @Test(expected = SchedulerException.class)
    public void getScheduler_NotStarted_Fails() throws Exception{
        Scheduler.stop();
        Scheduler.getScheduler();
    }

    @Test(expected = SchedulerException.class)
    public void stop_NotStarted_Fails() throws Exception{
        Scheduler.stop();
        Scheduler.stop();
    }
}
