import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import droneapi.api.DroneStatus;
import drones.models.Fleet;
import drones.scheduler.AdvancedScheduler;
import drones.scheduler.Helper;
import drones.scheduler.Scheduler;
import drones.scheduler.SchedulerException;
import drones.scheduler.messages.from.*;
import drones.scheduler.messages.to.SchedulerRequestMessage;
import models.*;
import org.junit.*;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import simulator.SimulatorDriver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import controllers.*;

import javax.persistence.OptimisticLockException;

import static play.mvc.Results.forbidden;

/**
 * Created by Ronald on 18/04/2015.
 */
public class AdvancedSchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    private static final FiniteDuration SHORT_TIMEOUT = Duration.create(2, TimeUnit.SECONDS);
    private static final FiniteDuration LONG_TIMEOUT = Duration.create(5, TimeUnit.SECONDS);
    // Our location
    private static final double DIST_GHENT_ANTWERP = 51474;
    private static final double DIST_ANTWERP_PARIS = 301065;
    private static final Location GHENT = new Location(51.05, 3.71667, 0);
    private static final Location ANTWERP = new Location(51.21989, 4.40346, 0);
    private static final Location PARIS = new Location(48.85341, 2.3488, 0);
    private static final Location BERLIN = new Location(52.52437, 13.41053, 0);
    private static final Location LONDON = new Location(51.50853, -0.12574, 0);
    private static final Location CITADELPARK = new Location(51.037824, 3.720594, 0);
    private static final Location IKEAGENT = new Location(51.022466, 3.687980, 0);
    private static final Location GRAVENSTEEN = new Location(51.057481, 3.720773, 0);
    private static final Location ZUIDERPOORT = new Location(51.036169, 3.735918, 0);
    private static final Location VOORUIT = new Location(51.047884, 3.727434, 0);
    private static final Location STERRE = new Location(51.026201, 3.710812, 0);
    private static final Location NORTH_WEST = new Location(0.01, -0.01, 0);
    private static final Location NORTH = new Location(0.01, 0, 0);
    private static final Location NORTH_EAST = new Location(0.01, 0.01, 0);
    private static final Location WEST = new Location(0, -0.01, 0);
    private static final Location CENTER = new Location(0, 0, 0);
    private static final Location EAST = new Location(0, 0.01, 0);
    private static final Location SOUTH_WEST = new Location(-0.01, -0.01, 0);
    private static final Location SOUTH = new Location(-0.01, 0, 0);
    private static final Location SOUTH_EAST = new Location(-0.01, 0.01, 0);

    // Base stations
    private static Basestation BRUSSELS;
    private static Basestation DELHI;
    private static Basestation KINSHASA;
    private static Basestation LIMA;
    private static Basestation MOSCOW;
    private static Basestation NEW_YORK;
    private static Basestation ROME;
    private static Basestation SEATTLE;
    private static Basestation SYDNEY;
    private static Basestation TOKYO;
    // List of these base stations
    private static final List<Basestation> BASESTATIONS = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        // Start application
        startFakeApplication();
        BRUSSELS = new Basestation("Brussels", new Location(50.85045, 4.34878, 0));
        DELHI = new Basestation("Delhi", new Location(28.65381, 77.22897, 0));
        KINSHASA = new Basestation("Kinshasa", new Location(-4.32758, 15.31357, 0));
        LIMA = new Basestation("Lima", new Location(-12.04318, -77.02824, 0));
        MOSCOW = new Basestation("Moscow", new Location(55.75222, 37.61556, 0));
        NEW_YORK = new Basestation("New York", new Location(40.71427, -74.00597, 0));
        ROME = new Basestation("Rome", new Location(41.89193, 12.51133, 0));
        SEATTLE = new Basestation("Seattle", new Location(47.60621, -122.33207, 0));
        SYDNEY = new Basestation("Sydney", new Location(-33.86785, 151.20732, 0));
        TOKYO = new Basestation("Tokyo", new Location(35.6895, 139.69171, 0));
        BASESTATIONS.add(BRUSSELS);
        BASESTATIONS.add(DELHI);
        BASESTATIONS.add(KINSHASA);
        BASESTATIONS.add(LIMA);
        BASESTATIONS.add(MOSCOW);
        BASESTATIONS.add(NEW_YORK);
        BASESTATIONS.add(ROME);
        BASESTATIONS.add(SEATTLE);
        BASESTATIONS.add(SYDNEY);
        BASESTATIONS.add(TOKYO);

        // Create system
        system = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
    }

    @Before
    public void before() {
        if (!setup) {
            Fleet.registerDriver(new DroneType(SimulatorDriver.SIMULATOR_TYPE), driver);
            setup = true;
        }
    }

    @After
    public void after() {
        // Clean DB
        Ebean.delete(Assignment.FIND.all());
        Ebean.delete(Drone.FIND.all());
        Ebean.delete(Basestation.FIND.all());
    }


    private List<Drone> createTestDrones(int number) {
        List<Drone> drones = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            drones.add(new Drone("TestDrone" + i, Drone.Status.UNKNOWN, new DroneType(SimulatorDriver.SIMULATOR_TYPE), "0.0.0.0"));
        }
        Ebean.save(drones);
        return drones;
    }

    @Test
    public void distance_GhentAntwerp_100MeterAccuracy() {
        double distance = Helper.distance(GHENT, ANTWERP);
        double error = Math.abs(DIST_GHENT_ANTWERP - distance);
        Assert.assertTrue(error < 100);
    }

    @Test
    public void closestBaseStation_Ghent_Brussels() {
        // Add stations
        Ebean.save(BASESTATIONS);
        Basestation closest = Helper.closestBaseStation(GHENT);
        Assert.assertTrue(closest.equals(BRUSSELS));
        // Remove stations
        Ebean.delete(BASESTATIONS);
    }

    @Test
    public void routeLength_GhentAntwerpParis_1KilometerAccuracy() {
        List<Checkpoint> route = new ArrayList<>();
        route.add(new Checkpoint(GHENT));
        route.add(new Checkpoint(ANTWERP));
        route.add(new Checkpoint(PARIS));
        Assignment assignment = new Assignment(route, getUser());
        double length = Helper.getRouteLength(assignment);
        double error = Math.abs(DIST_GHENT_ANTWERP + DIST_ANTWERP_PARIS - length);
        Assert.assertTrue(error < 1000);
    }

    @Test
    public void routeLength_InvalidRoute_NaN() {
        double length1 = Helper.getRouteLength(new Assignment());
        double length2 = Helper.getRouteLength(new Assignment(new ArrayList<>(), getUser()));
        Assert.assertTrue(Double.isNaN(length1));
        Assert.assertTrue(Double.isNaN(length2));
    }

    @Test
    public void routeLength_Ghent_Zero() {
        List<Checkpoint> route = Helper.routeTo(GHENT);
        Assignment assignment = new Assignment(route, getUser());
        double length = Helper.getRouteLength(assignment);
        Assert.assertTrue(length <= 0);
        Assert.assertTrue(length >= 0);
    }

    @Test
    public void subscriberTest_RequestMessage_ReplyMessage() {
        new JavaTestKit(system) {
            {
                subscribe(this, SchedulerReplyMessage.class);
                ActorRef scheduler = Scheduler.getScheduler();
                SchedulerRequestMessage request = new SchedulerRequestMessage();
                scheduler.tell(request, getRef());
                SchedulerReplyMessage reply = expectMsgClass(SchedulerReplyMessage.class);
                Assert.assertTrue(request.getRequestId() == reply.getRequestId());
                unsubscribe(this, SchedulerReplyMessage.class);
            }
        };
    }

    private Drone createDrone(String name, Location location, Drone.Status status) {
        Drone drone = new Drone(name, status, new DroneType(SimulatorDriver.SIMULATOR_TYPE), "0.0.0.0");
        drone.save();
        driver.setStartLocation(Helper.entityToDroneLocation(location));
        Fleet.getFleet().createCommanderForDrone(drone);
        return drone;
    }

    private void deleteDrone(Drone drone) {
        Fleet.getFleet().stopCommander(drone);
        drone.delete();
    }

    @Test
    public void scheduleAssignment_closest_succeeds() {
        new JavaTestKit(system) {
            {
                Drone closest = createDrone("Center", CENTER, Drone.Status.AVAILABLE);
                createDrone("North West", NORTH_WEST, Drone.Status.AVAILABLE);
                createDrone("North", NORTH, Drone.Status.AVAILABLE);
                createDrone("North East", NORTH_EAST, Drone.Status.AVAILABLE);

                subscribe(this, DroneAssignedMessage.class);
                subscribe(this, AssignmentStartedMessage.class);
                subscribe(this, DroneStatusMessage.class);
                Assignment assignment = createAssignment(SOUTH);
                Scheduler.scheduleAssignment(assignment.getId());

                DroneAssignedMessage A = expectMsgClass(DroneAssignedMessage.class);
                Assert.assertTrue("Correct assignment id", A.getAssignmentId() == assignment.getId());
                Assert.assertTrue("Correct drone id", A.getDroneId() == closest.getId());
                assignment.refresh();
                Assert.assertTrue("Assignment scheduled", assignment.isScheduled());
                Assert.assertTrue("Closest assigned", closest.getId() == assignment.getAssignedDrone().getId());

                DroneStatusMessage B = expectMsgClass(DroneStatusMessage.class);
                Assert.assertTrue("Correct drone id", B.getDroneId() == closest.getId());
                Assert.assertTrue("Message status FLYING", B.getNewStatus() == Drone.Status.FLYING);
                closest.refresh();
                Assert.assertTrue("Drone status FLYING", closest.getStatus() == Drone.Status.FLYING);

                AssignmentStartedMessage C = expectMsgClass(AssignmentStartedMessage.class);
                Assert.assertTrue("Correct assignment id", C.getAssignmentId() == assignment.getId());
            }
        };
    }

    private void subscribe(JavaTestKit test, Class<? extends SchedulerEvent> eventType) {
        Scheduler.subscribe(eventType, test.getRef());
        SubscribedMessage A = test.expectMsgClass(SubscribedMessage.class);
        Assert.assertTrue(A.getEventType() == eventType);
    }

    private void unsubscribe(JavaTestKit test, Class<? extends SchedulerEvent> eventType) {
        Scheduler.unsubscribe(eventType, test.getRef());
        UnsubscribedMessage A = test.expectMsgClass(UnsubscribedMessage.class);
        Assert.assertTrue(A.getEventType() == eventType);
    }

    private Assignment createAssignment(Location location) {
        Assignment assignment = new Assignment(Helper.routeTo(location), getUser());
        assignment.save();
        return assignment;
    }

    public Basestation createBasestation(String name, Location location) {
        Basestation basestation = new Basestation(name, location);
        basestation.save();
        return basestation;
    }
}