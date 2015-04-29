import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.flightcontrol.SimpleControlTower;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.Helper;
import drones.models.scheduler.messages.to.FlightCompletedMessage;
import drones.simulation.SimulatorDriver;
import models.Checkpoint;
import models.Drone;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by Sander on 15/04/2015.
 */
public class ControlTowerTest extends TestSuperclass {

    private static final models.Location DRONE1 = new models.Location(51.0226, 3.71, 0);
    private static final models.Location DRONE2 = new models.Location(51.0226, 3.73, 0);
    private static final models.Location DRONE3 = new models.Location(51.0226, 3.75, 0);

    public static final FiniteDuration MAX_DURATION_MESSAGE = Duration.create(10, "seconds");
    public static final FiniteDuration MAX_DURATION_FLYING = Duration.create(120, "seconds");

    private static ActorSystem system;
    private static List<Drone> drones = new ArrayList<>();
    private static List<DroneCommander> droneCommanders = new ArrayList<>();

    public ControlTowerTest() throws Exception {
        Fleet fleet = Fleet.getFleet();

        //add drones
        driver.setStartLocation(Helper.entityToDroneLocation(DRONE1));
        Drone drone1 = new Drone("TestDrone1", Drone.Status.UNKNOWN,SimulatorDriver.SIMULATOR_TYPE,"0.0.0.0");
        drone1.save();
        drones.add(drone1);
        droneCommanders.add(Await.result(fleet.createCommanderForDrone(drone1), MAX_DURATION_MESSAGE));
        driver.setStartLocation(Helper.entityToDroneLocation(DRONE2));
        Drone drone2 = new Drone("TestDrone2", Drone.Status.UNKNOWN,SimulatorDriver.SIMULATOR_TYPE,"0.0.0.0");
        drone2.save();
        drones.add(drone2);
        droneCommanders.add(Await.result(fleet.createCommanderForDrone(drone2), MAX_DURATION_MESSAGE));
        driver.setStartLocation(Helper.entityToDroneLocation(DRONE3));
        Drone drone3 = new Drone("TestDrone3", Drone.Status.UNKNOWN,SimulatorDriver.SIMULATOR_TYPE,"0.0.0.0");
        drone3.save();
        drones.add(drone3);
        droneCommanders.add(Await.result(fleet.createCommanderForDrone(drone3), MAX_DURATION_MESSAGE));
    }

    @BeforeClass
    public static void setup() throws Exception {
        startFakeApplication();
        system = ActorSystem.create();
    }

    @After
    public void after(){
        // Clean DB
        Ebean.delete(Drone.FIND.all());
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
    }

    /**
     * SimpleControlTower with one drone
     */
    @Test
    public void flyOneDrone(){
        new JavaTestKit(system) {
            {
                //create SimpleControlTower
                final ActorRef simpleControlTower = system.actorOf(
                        Props.create(SimpleControlTower.class,
                                () -> new SimpleControlTower(getRef(),10,5,1))
                );

                simpleControlTower.tell(new StartFlightControlMessage(), getRef());

                List<Checkpoint> wayPoints = new ArrayList<>();
                wayPoints.add(new Checkpoint(51.0226, 3.72, 0));
                wayPoints.add(new Checkpoint(51.0226, 3.73, 0));

                //start a new flight
                simpleControlTower.tell(new AddFlightMessage(drones.get(0).getId(),wayPoints),getRef());

                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, FlightCompletedMessage.class);

                simpleControlTower.tell(new StopFlightControlMessage(), getRef());
            }
        };
    }
}
