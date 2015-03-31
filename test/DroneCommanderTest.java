import akka.actor.ActorRef;
import akka.actor.Props;
import drones.models.DroneCommander;
import drones.models.DroneDriver;
import drones.models.DroneException;
import drones.simulation.SimulatorDriver;
import org.junit.*;
import play.libs.Akka;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Created by yasser on 25/03/15.
 */
public class DroneCommanderTest extends TestSuperclass {

    private static final Duration TIME_OUT = Duration.create(5, TimeUnit.SECONDS);

    @BeforeClass
    public static void setup() {
        startFakeApplication();
    }

    @AfterClass
    public static void tearDown() {
        stopFakeApplication();
    }

    private static final DroneDriver driver = new SimulatorDriver();
    private DroneCommander uninitializedCommander;
    private DroneCommander initializedCommander;

    private DroneCommander createDroneCommander() {
        // Create Commander
        ActorRef ref = Akka.system().actorOf(
                Props.create(driver.getActorClass(),
                        () -> driver.createActor(null)));
        return new DroneCommander(ref);
    }

    @Before
    public void setCommander() throws Exception {

        uninitializedCommander = createDroneCommander();
        DroneCommander commander = createDroneCommander();
        Await.result(commander.init(), TIME_OUT);
        initializedCommander = commander;
    }

    // Take-off

    @Test(expected = DroneException.class)
    public void takeOff_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.takeOff();
        Await.result(future, TIME_OUT);
    }

    @Test
    public void takeOff_Initialized_Succeeds() throws Exception {
        Future future = initializedCommander.takeOff();
        Await.result(future, TIME_OUT);
    }

    // Land

    @Test(expected = DroneException.class)
    public void land_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.land();
        Await.result(future, TIME_OUT);
    }

    @Test
    public void land_Initialized_Succeeds() throws Exception {
        Future future = initializedCommander.land();
        Await.result(future, TIME_OUT);
    }

    // Move 3D

    @Test(expected = DroneException.class)
    public void move3d_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.move3d(1, 1, 1, 1);
        Await.result(future, TIME_OUT);
    }

    @Ignore
    @Test
    public void move3d_Initialized_Succeeds() throws Exception {
        DroneCommander commander = createDroneCommander();
        Await.ready(commander.init(), TIME_OUT);
        Await.ready(commander.takeOff(), TIME_OUT);
        Future future = commander.move3d(1, 1, 1, 1);
        Await.result(future, TIME_OUT);
    }

    // Move

    @Test(expected = DroneException.class)
    public void move_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.move(1, 1, 1);
        Await.result(future, TIME_OUT);
    }

    @Ignore
    @Test
    public void move_Initialized_Succeeds() throws Exception {
        DroneCommander commander = createDroneCommander();
        Await.ready(commander.init(), TIME_OUT);
        Await.ready(commander.takeOff(), TIME_OUT);
        Future future = commander.move(1, 2, 3);
        Await.result(future, TIME_OUT);
    }

    // Set Max-Height

    @Test(expected = DroneException.class)
    public void setMaxHeight_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.setMaxHeight(20);
        Await.result(future, TIME_OUT);
    }

    @Test
    public void setMaxHeight_Initialized_Succeeds() throws Exception {
        Future future = initializedCommander.setMaxHeight(20);
        Await.result(future, TIME_OUT);
    }

    // Set Max-Tilt

    @Test(expected = DroneException.class)
    public void setMaxTilt_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.setMaxTilt(45);
        Await.result(future, TIME_OUT);
    }

    @Test
    public void setMaxTilt_Initialized_Succeeds() throws Exception {
        Future future = initializedCommander.setMaxTilt(45);
        Await.result(future, TIME_OUT);
    }

    // Move to location

    @Test(expected = DroneException.class)
    public void moveToLocation_Uninitialized_Fails() throws Exception {
        initializedCommander.takeOff();
        Future future = uninitializedCommander.moveToLocation(20, 30, 20);
        Await.result(future, TIME_OUT);
        initializedCommander.land();
    }

    @Test
    public void moveToLocation_Initialized_Succeeds() throws Exception {
        DroneCommander commander = createDroneCommander();
        Await.ready(commander.init(), TIME_OUT);
        Await.ready(commander.takeOff(), TIME_OUT);
        Future future = commander.moveToLocation(10, 20, 30);
        Await.result(future, TIME_OUT);
    }

    // Cancel move to location

    @Test(expected = DroneException.class)
    public void cancelMoveToLocation_Uninitialized_Fails() throws Exception {
        Future future = uninitializedCommander.cancelMoveToLocation();
        Await.result(future, TIME_OUT);
    }

    @Test
    public void cancelMoveToLocation_Initialized_Succeeds() throws Exception {
        Future future = initializedCommander.cancelMoveToLocation();
        Await.result(future, TIME_OUT);
    }

    @Test
    public void init_Simulator_Succeeds() throws Exception {

        DroneCommander newCommander = createDroneCommander();
        Future future = newCommander.init();
        Await.result(future, TIME_OUT);
    }

    // Write test for gets

    // Write tests for subscribers
}
