import CollisionDetector.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.Fleet;
import drones.flightcontrol.SimpleControlTower;
import drones.flightcontrol.messages.*;
import drones.scheduler.Helper;
import drones.scheduler.messages.to.FlightCompletedMessage;
import models.Checkpoint;
import models.Drone;
import models.DroneType;
import models.Location;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import simulator.SimulatorDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sander on 15/04/2015.
 */
public class ControlTowerTest extends TestSuperclass {

    private static final models.Location STERRE = new models.Location(51.0226, 3.71, 0);
    private static final Location NORTH = new Location(0.01,0,0);
    private static final Location SOUTH = new Location(-0.01,0,0);
    private static final Location WEST = new Location(0,-0.01,0);
    private static final Location CENTER = new Location(0,0,0);

    public static final FiniteDuration MAX_DURATION_MESSAGE = Duration.create(10, "seconds");
    public static final FiniteDuration MAX_DURATION_FLYING = Duration.create(180, "seconds");

    private static ActorSystem system;

    public ControlTowerTest() {
        driver.setTopSpeed(100);
    }

    private Drone addDrone(models.Location location) throws Exception {
        Fleet fleet = Fleet.getFleet();
        driver.setStartLocation(Helper.entityToDroneLocation(location));
        Drone drone = new Drone("TestDrone", Drone.Status.UNKNOWN, new DroneType(SimulatorDriver.SIMULATOR_TYPE),"0.0.0.0");
        drone.save();
        Await.result(fleet.createCommanderForDrone(drone), MAX_DURATION_MESSAGE);
        return drone;
    }

    @BeforeClass
    public static void setup() throws Exception {
        startFakeApplication();
        system = ActorSystem.create();
    }

    @Before
    public void before(){
        if(!setup) {
            Fleet.registerDriver(new DroneType(SimulatorDriver.SIMULATOR_TYPE), driver);
            setup = true;
        }
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
    public void flyOneDrone() throws Exception {
        Drone drone = addDrone(STERRE);
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
                simpleControlTower.tell(new AddFlightMessage(drone.getId(),wayPoints),getRef());

                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, FlightCompletedMessage.class);

                simpleControlTower.tell(new StopFlightControlMessage(), getRef());
            }
        };
    }


    /**
     * Test with 2 drones
     */
    @Test
    public void flyTwoDrones() throws Exception {
        ArrayList<Long> drones = new ArrayList<>();

        //Drone that will fly from Nord to South
        Drone droneNS = addDrone(NORTH);
        drones.add(droneNS.getId());
        //Drone that will fly from West to Center
        Drone droneWC = addDrone(WEST);
        drones.add(droneWC.getId());

        List<Checkpoint> wayPointsDroneNS = new ArrayList<>();
        wayPointsDroneNS.add(new Checkpoint(SOUTH));

        List<Checkpoint> wayPointsDroneWC = new ArrayList<>();
        wayPointsDroneWC.add(new Checkpoint(CENTER));

        new JavaTestKit(system) {
            {
                //create SimpleControlTower
                final ActorRef simpleControlTower = system.actorOf(
                        Props.create(SimpleControlTower.class,
                                () -> new SimpleControlTower(getRef(),10,5,2))
                );

                //create CollisionDetector
                final ActorRef collisionDetector = system.actorOf(
                        Props.create(CollisionDetector.class,
                                () -> new CollisionDetector(drones,getRef()))
                );


                simpleControlTower.tell(new StartFlightControlMessage(), getRef());

                //start a new flight
                simpleControlTower.tell(new AddFlightMessage(droneNS.getId(),wayPointsDroneNS),getRef());
                simpleControlTower.tell(new AddFlightMessage(droneWC.getId(),wayPointsDroneWC),getRef());

                expectMsgClass(MAX_DURATION_FLYING, WayPointCompletedMessage.class);
                expectMsgAnyClassOf(MAX_DURATION_FLYING,WayPointCompletedMessage.class,FlightCompletedMessage.class);
                expectMsgAnyClassOf(MAX_DURATION_FLYING,WayPointCompletedMessage.class,FlightCompletedMessage.class,RemoveFlightCompletedMessage.class);
                expectMsgAnyClassOf(MAX_DURATION_FLYING,WayPointCompletedMessage.class,RemoveFlightCompletedMessage.class);
                expectMsgClass(MAX_DURATION_FLYING, FlightCompletedMessage.class);

                simpleControlTower.tell(new StopFlightControlMessage(), getRef());

                //stop collision detector
                collisionDetector.tell(new CollisionDetectorStopMessage(), getRef());
                expectMsgEquals(MAX_DURATION_MESSAGE, false);
            }
        };
    }
}