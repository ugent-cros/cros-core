import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import drones.models.Fleet;
import drones.models.scheduler.Helper;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.SchedulerException;
import drones.models.scheduler.SimpleScheduler;
import drones.models.scheduler.messages.to.AssignmentMessage;
import drones.simulation.SimulatorDriver;
import models.Assignment;
import models.Drone;
import models.Location;
import org.junit.*;

/**
 * Created by Ronald on 6/04/2015.
 */
public class SchedulerTest extends TestSuperclass {

    private static final Location ANTWERP = new Location(51.21989,4.40346,0);
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

    @Override
    public void before() {
        startFakeApplication();
        super.before();
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
        Assignment assignment = new Assignment(Helper.routeTo(ANTWERP),getUser());
        assignment.save();

        Drone drone = new Drone("Simulator", Drone.Status.AVAILABLE, SimulatorDriver.SIMULATOR_TYPE,"x.x.x.x");
        drone.save();

        Scheduler.getScheduler().tell(new AssignmentMessage(assignment.getId()), ActorRef.noSender());
        Thread.sleep(3000);
        drone.refresh();
        assignment.refresh();
        Assert.assertTrue("Drone assigned", assignment.getAssignedDrone() != null);
        Drone assignedDrone = assignment.getAssignedDrone();
        Assert.assertTrue("Correct drone assigned", assignedDrone.getId() == drone.getId());
        Assert.assertTrue(drone.getStatus() == Drone.Status.FLYING);

    }
}
