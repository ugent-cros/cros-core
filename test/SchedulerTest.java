import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.DroneException;
import drones.models.flightcontrol.messages.StartFlightControlMessage;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.SchedulerException;
import drones.models.scheduler.SimpleScheduler;
import drones.models.scheduler.messages.AssignmentMessage;
import drones.simulation.SimulatorDriver;
import models.Assignment;
import models.Drone;
import models.DroneType;
import org.junit.*;

import scala.concurrent.duration.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 6/04/2015.
 */
public class SchedulerTest extends TestSuperclass {

    static ActorSystem system;

    @BeforeClass
    public static void createSystem() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void destroySystem() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void setup() {
        startFakeApplication();
    }

    @After
    public void tearDown() {
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

    @Test
    public void scheduleAssignment_Succeeds() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setId(13l);
        assignment.setProgress(0);
        assignment.save();

        Drone drone = new Drone("Simulator", Drone.Status.AVAILABLE, SimulatorDriver.SIMULATOR_TYPE,"x.x.x.x");
        drone.setId(14l);
        drone.save();

        Scheduler.getScheduler().tell(new AssignmentMessage(assignment.getId()), null);
        Thread.sleep(3000);
        drone = Drone.FIND.byId(drone.getId());
        assignment = Assignment.FIND.byId(assignment.getId());
        Assert.assertTrue(assignment.getAssignedDrone().getId() == drone.getId());
        Assert.assertTrue(drone.getStatus() == Drone.Status.UNAVAILABLE);

    }
}
