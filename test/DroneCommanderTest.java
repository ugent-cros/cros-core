import drones.models.*;
import models.Drone;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import simulation.SimulatorDriver;

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

    private Drone testDrone;

    @Before
    public void createCommander() {

        testDrone = new Drone("simulator", Drone.Status.AVAILABLE, SimulatorDriver.SIMULATOR_TYPE, "x");
        testDrone.save();

        DroneDriver driver  = new SimulatorDriver();
        Fleet.registerDriver(SimulatorDriver.SIMULATOR_TYPE, driver);
    }

    @Test(expected = DroneException.class)
    public void takeOff_WithOutInit_Fails() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.takeOff();
        Await.result(future, TIME_OUT);
    }

    @Test(expected = DroneException.class)
    public void land_WithOutInit_Fails() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.land();
        Await.result(future, TIME_OUT);
    }

    @Test(expected = DroneException.class)
    public void setMaxHeight_WithOutInit_Fails() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.setMaxHeight(20);
        Await.result(future, TIME_OUT);
    }

    @Test(expected = DroneException.class)
    public void moveToLocation_WithOutInit_Fails() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.moveToLocation(20, 30, 20);
        Await.result(future, TIME_OUT);
    }

    @Test
    public void init_Simulator_Succeeds() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.init();
        Await.result(future, TIME_OUT);
    }

    @Test
    public void calibrate_AfterInit_Succeeds() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.init();
        Await.result(future, TIME_OUT);
        future = commander.calibrate(false, true);
        Await.result(future, TIME_OUT);
    }

    @Test
    public void getState_AfterInit_Succeeds() throws Exception {

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(testDrone);
        Future future = commander.init();
        Await.result(future, TIME_OUT);
        future = commander.getFlyingState();
        FlyingState state = (FlyingState) Await.result(future, TIME_OUT);
    }
}
