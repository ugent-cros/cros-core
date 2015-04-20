import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.scheduler.Helper;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.SchedulerException;
import drones.models.scheduler.messages.from.DroneAddedMessage;
import drones.models.scheduler.messages.from.DroneAssignedMessage;
import drones.models.scheduler.messages.from.AssignmentCompletedMessage;
import drones.models.scheduler.messages.from.SchedulerReplyMessage;
import drones.models.scheduler.messages.to.SchedulerRequestMessage;
import drones.simulation.SimulatorDriver;
import models.*;
import org.junit.*;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 18/04/2015.
 */
public class AdvancedSchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    private static final FiniteDuration SHORT_TIMEOUT = Duration.create(2, TimeUnit.SECONDS);
    private static final FiniteDuration LONG_TIMEOUT = Duration.create(10, TimeUnit.SECONDS);
    // Our location
    private static final double DIST_GHENT_ANTWERP = 51474;
    private static final double DIST_ANTWERP_PARIS = 301065;
    private static final Location GHENT = new Location(3.71667,51.05,0);
    private static final Location ANTWERP = new Location(4.40346,51.21989,0);
    private static final Location PARIS = new Location(2.3488,48.85341,0);
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

    @BeforeClass
    public static void setup(){
        startFakeApplication();
        BRUSSELS = new Basestation("Brussels", new Location(4.34878,50.85045,0));
        DELHI = new Basestation("Delhi", new Location(77.22897,28.65381,0));
        KINSHASA = new Basestation("Kinshasa", new Location(15.31357,-4.32758,0));
        LIMA = new Basestation("Lima", new Location(-77.02824,-12.04318,0));
        MOSCOW = new Basestation("Moscow", new Location(37.61556,55.75222,0));
        NEW_YORK = new Basestation("New York", new Location(-74.00597,40.71427,0));
        ROME = new Basestation("Rome", new Location(12.51133,41.89193,0));
        SEATTLE = new Basestation("Seattle", new Location(-122.33207,47.60621,0));
        SYDNEY = new Basestation("Sydney", new Location(151.20732,-33.86785,0));
        TOKYO = new Basestation("Tokyo", new Location(139.69171,35.6895,0));
        List<Basestation> basestations = new ArrayList<>();
        basestations.add(BRUSSELS);
        basestations.add(DELHI);
        basestations.add(KINSHASA);
        basestations.add(LIMA);
        basestations.add(MOSCOW);
        basestations.add(NEW_YORK);
        basestations.add(ROME);
        basestations.add(SEATTLE);
        basestations.add(SYDNEY);
        basestations.add(TOKYO);
        Ebean.save(basestations);

        List<Drone> drones = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Drone drone = new Drone("Drone" + i, Drone.Status.AVAILABLE, SimulatorDriver.SIMULATOR_TYPE,"0.0.0." + i);
            drones.add(drone);
        }
        Ebean.save(drones);

        // Create system
        system = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown(){
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
    }

    @Test
    public void distance_GhentAntwerp_100MeterAccuracy(){
        double distance = Helper.distance(GHENT,ANTWERP);
        double error = Math.abs(DIST_GHENT_ANTWERP - distance);
        Assert.assertTrue(error < 100);
    }

    @Test
    public void closestBaseStation_Ghent_Brussels(){
        Basestation closest = Helper.closestBaseStation(GHENT);
        Assert.assertTrue(closest.equals(BRUSSELS));
    }

    @Test
    public void routeLength_GhentAntwerpParis_1KilometerAccuracy(){
        List<Checkpoint> route = new ArrayList<>();
        route.add(new Checkpoint(GHENT));
        route.add(new Checkpoint(ANTWERP));
        route.add(new Checkpoint(PARIS));
        Assignment assignment = new Assignment(route,getUser());
        double length = Helper.getRouteLength(assignment);
        double error = Math.abs(DIST_GHENT_ANTWERP + DIST_ANTWERP_PARIS - length);
        Assert.assertTrue(error < 1000);
    }

    @Test
    public void routeLength_InvalidRoute_NaN(){
        double length1 = Helper.getRouteLength(new Assignment());
        double length2 = Helper.getRouteLength(new Assignment(new ArrayList<>(),getUser()));
        Assert.assertTrue(Double.isNaN(length1));
        Assert.assertTrue(Double.isNaN(length2));
    }

    @Test
    public void routeLength_Ghent_Zero(){
        List<Checkpoint> route = Helper.routeTo(GHENT);
        Assignment assignment = new Assignment(route,getUser());
        double length = Helper.getRouteLength(assignment);
        Assert.assertTrue(length <= 0);
        Assert.assertTrue(length >= 0);
    }

    @Test
    public void subscriberTest_RequestMessage_ReplyMessage() throws SchedulerException {
        new JavaTestKit(system){
            {
                Scheduler.subscribe(SchedulerReplyMessage.class, getRef());
                ActorRef scheduler = Scheduler.getScheduler();
                SchedulerRequestMessage request = new SchedulerRequestMessage();
                scheduler.tell(request,getRef());
                SchedulerReplyMessage reply = expectMsgClass(SchedulerReplyMessage.class);
                Assert.assertTrue(request.getRequestId() == reply.getRequestId());
            }
        };
    }

    @Test
    public void addDrone_11Requests_10Added() throws SchedulerException{
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAddedMessage.class, getRef());

                // Add the drones
                List<Drone> drones = Drone.FIND.all();
                Set<Long> droneIdCheck = new HashSet<>();
                for (Drone drone : drones){
                    Scheduler.addDrone(drone.getId());
                    droneIdCheck.add(drone.getId());
                }
                // Expect 10 unique added drones.
                Object[] messages = receiveN(drones.size());
                for(Object message : messages){
                    long droneId = ((DroneAddedMessage) message).getDroneId();
                    Assert.assertTrue(droneIdCheck.remove(droneId));
                }
                Assert.assertTrue(droneIdCheck.isEmpty());

                // Send already added drone to add.
                Scheduler.addDrone(drones.get(0).getId());
                expectNoMsg(SHORT_TIMEOUT);
            }
        };
    }

    @Test
    public void schedule_1Assignment10Drones_1Scheduled() throws SchedulerException{
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAssignedMessage.class, getRef());
                Scheduler.subscribe(AssignmentCompletedMessage.class,getRef());
                // Add the drones
                List<Drone> drones = Drone.FIND.all();
                for (Drone drone : drones){
                    Scheduler.addDrone(drone.getId());
                }
                // Add the assignment
                Assignment assignment = new Assignment(Helper.routeTo(PARIS),getUser());
                assignment.save();

                // Schedule!
                Scheduler.schedule();
                DroneAssignedMessage message = expectMsgClass(DroneAssignedMessage.class);
                Assert.assertTrue(message.getAssignmentId() == assignment.getId());
                assignment = Assignment.FIND.byId(message.getAssignmentId());
                Assert.assertTrue(assignment.isScheduled());
                Drone drone = Drone.FIND.byId(message.getDroneId());
                Assert.assertTrue(drone.getStatus() == Drone.Status.FLYING);
            }
        };
    }


}
