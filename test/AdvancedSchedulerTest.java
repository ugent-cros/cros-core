import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.Fleet;
import drones.models.scheduler.AdvancedScheduler;
import drones.models.scheduler.Helper;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.SchedulerException;
import drones.models.scheduler.messages.from.*;
import drones.models.scheduler.messages.to.SchedulerRequestMessage;
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

/**
 * Created by Ronald on 18/04/2015.
 */
@Ignore
public class AdvancedSchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    private static final FiniteDuration SHORT_TIMEOUT = Duration.create(2, TimeUnit.SECONDS);
    private static final FiniteDuration LONG_TIMEOUT = Duration.create(5, TimeUnit.SECONDS);
    // Our location
    private static final double DIST_GHENT_ANTWERP = 51474;
    private static final double DIST_ANTWERP_PARIS = 301065;
    private static final Location GHENT = new Location(51.05,3.71667,0);
    private static final Location ANTWERP = new Location(51.21989,4.40346,0);
    private static final Location PARIS = new Location(48.85341,2.3488,0);
    private static final Location BERLIN = new Location(52.52437,13.41053,0);
    private static final Location LONDON = new Location(51.50853,-0.12574,0);
    private static final Location CITADELPARK = new Location(51.037824,3.720594,0);
    private static final Location IKEAGENT = new Location(51.022466, 3.687980,0);
    private static final Location GRAVENSTEEN = new Location(51.057481, 3.720773,0);
    private static final Location ZUIDERPOORT = new Location(51.036169, 3.735918,0);
    private static final Location VOORUIT = new Location(51.047884, 3.727434,0);
    private static final Location STERRE = new Location(51.026201, 3.710812, 0);
    private static final Location NORTH_WEST = new Location(0.01,-0.01,0);
    private static final Location NORTH = new Location(0.01,0,0);
    private static final Location NORTH_EAST = new Location(0.01,0.01,0);
    private static final Location WEST = new Location(0,-0.01,0);
    private static final Location CENTER = new Location(0,0,0);
    private static final Location EAST = new Location(0,0.01,0);
    private static final Location SOUTH_WEST = new Location(-0.01,-0.01,0);
    private static final Location SOUTH = new Location(-0.01,0,0);
    private static final Location SOUTH_EAST = new Location(-0.01,0.01,0);

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
    public static void setup(){
        // Start application
        startFakeApplication();
        // Make sure we are using the Advanced Scheduler!
        try {
            Scheduler.stop();
            Scheduler.start(AdvancedScheduler.class);
        }catch(SchedulerException ex){
            throw new RuntimeException("Failed to start Advanced Scheduler.");
        }
        BRUSSELS = new Basestation("Brussels", new Location(50.85045,4.34878,0));
        DELHI = new Basestation("Delhi", new Location(28.65381,77.22897,0));
        KINSHASA = new Basestation("Kinshasa", new Location(-4.32758,15.31357,0));
        LIMA = new Basestation("Lima", new Location(-12.04318,-77.02824,0));
        MOSCOW = new Basestation("Moscow", new Location(55.75222,37.61556,0));
        NEW_YORK = new Basestation("New York", new Location(40.71427,-74.00597,0));
        ROME = new Basestation("Rome", new Location(41.89193,12.51133,0));
        SEATTLE = new Basestation("Seattle", new Location(47.60621,-122.33207,0));
        SYDNEY = new Basestation("Sydney", new Location(-33.86785,151.20732,0));
        TOKYO = new Basestation("Tokyo", new Location(35.6895,139.69171,0));
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
    public static void tearDown(){
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
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
        Ebean.delete(Assignment.FIND.all());
        Ebean.delete(Drone.FIND.all());
        Ebean.delete(Basestation.FIND.all());
    }

    private Drone createDrone(Location location) throws SchedulerException{
        driver.setStartLocation(Helper.entityToDroneLocation(location));
        Drone drone = new Drone("TestDrone", Drone.Status.UNKNOWN,new DroneType(new DroneType(SimulatorDriver.SIMULATOR_TYPE)),"0.0.0.0");
        drone.save();
        Scheduler.addDrone(drone.getId());
        return drone;
    }

    private List<Drone> createTestDrones(int number){
        List<Drone> drones = new ArrayList<>();
        for(int i = 0; i < number; i++){
            drones.add(new Drone("TestDrone" + i, Drone.Status.UNKNOWN,new DroneType(new DroneType(SimulatorDriver.SIMULATOR_TYPE)),"0.0.0.0"));
        }
        Ebean.save(drones);
        return drones;
    }

    @Test
    public void distance_GhentAntwerp_100MeterAccuracy(){
        double distance = Helper.distance(GHENT,ANTWERP);
        double error = Math.abs(DIST_GHENT_ANTWERP - distance);
        Assert.assertTrue(error < 100);
    }

    @Test
    public void closestBaseStation_Ghent_Brussels(){
        // Add stations
        Ebean.save(BASESTATIONS);
        Basestation closest = Helper.closestBaseStation(GHENT);
        Assert.assertTrue(closest.equals(BRUSSELS));
        // Remove stations
        Ebean.delete(BASESTATIONS);
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
                subscribe(this, SchedulerReplyMessage.class);
                ActorRef scheduler = Scheduler.getScheduler();
                SchedulerRequestMessage request = new SchedulerRequestMessage();
                scheduler.tell(request,getRef());
                SchedulerReplyMessage reply = expectMsgClass(SchedulerReplyMessage.class);
                Assert.assertTrue(request.getRequestId() == reply.getRequestId());
                unsubscribe(this, SchedulerReplyMessage.class);
            }
        };
    }

    @Test
    public void addDrones_FilledDB_Succeeds() throws SchedulerException{
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAddedMessage.class, getRef());
                Scheduler.subscribe(DroneRemovedMessage.class, getRef());

                // Add drones to the database.
                List<Drone> drones = createTestDrones(10);

                Scheduler.addDrones();
                for(int i = 0; i < drones.size(); i++){
                    DroneAddedMessage message = expectMsgClass(DroneAddedMessage.class);
                    Assert.assertTrue("Drone added successfully",message.isSuccess());
                }

                // Check drones
                drones = Drone.FIND.all();
                for(Drone drone : drones){
                    Assert.assertTrue("Drone status AVAILABLE",drone.getStatus() == Drone.Status.AVAILABLE);
                    Scheduler.removeDrone(drone.getId());
                }
                // Removed drone messages
                receiveN(drones.size());
            }
        };
    }

    @Test
    public void droneAddRemove_EmptyDB_Succeeds() throws SchedulerException{
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAddedMessage.class, getRef());
                Scheduler.subscribe(DroneRemovedMessage.class, getRef());
                // Add the drones
                List<Drone> drones = createTestDrones(10);
                Set<Long> droneIdCheck = new HashSet<>();
                Drone drone = null;
                for (Drone d : drones){
                    Scheduler.addDrone(d.getId());
                    droneIdCheck.add(d.getId());
                }
                // Expect 10 unique added drones.
                Object[] messages = receiveN(drones.size());
                for(Object message : messages){
                    DroneAddedMessage addedMessage = ((DroneAddedMessage) message);
                    long droneId = addedMessage.getDroneId();
                    Assert.assertTrue("Drone added successful",addedMessage.isSuccess());
                    Assert.assertTrue("Unique drone added",droneIdCheck.remove(droneId));
                    drone = Drone.FIND.byId(droneId);
                    Assert.assertTrue("Drone status AVAILABLE",drone.getStatus() == Drone.Status.AVAILABLE);
                }
                Assert.assertTrue("All drones added",droneIdCheck.isEmpty());

                // Send already added drone to add.
                Scheduler.addDrone(drones.get(0).getId());
                expectNoMsg(SHORT_TIMEOUT);

                // Removed drones
                for(Drone d : drones){
                    Scheduler.removeDrone(d.getId());
                    droneIdCheck.add(d.getId());
                }
                // Expect 10 unique removed drones.
                messages = receiveN(drones.size());
                for(Object message : messages){
                    long droneId = ((DroneRemovedMessage) message).getDroneId();
                    drone = Drone.FIND.byId(droneId);
                    Assert.assertTrue("Unique drone removed",droneIdCheck.remove(droneId));
                    Assert.assertTrue("Drone status INACTIVE",drone.getStatus() == Drone.Status.INACTIVE);
                }
                Assert.assertTrue("All drones removed",droneIdCheck.isEmpty());

                // Send already removed drone to remove.
                Scheduler.removeDrone(drones.get(0).getId());
                expectNoMsg(SHORT_TIMEOUT);
            }
        };
    }

    @Ignore
    @Test
    public void schedule_RemoveDrone_Succeeds() throws SchedulerException{
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAddedMessage.class, getRef());
                Scheduler.subscribe(DroneRemovedMessage.class, getRef());
                Scheduler.subscribe(DroneAssignedMessage.class, getRef());
                Scheduler.subscribe(DroneUnassignedMessage.class, getRef());
                Scheduler.subscribe(AssignmentCompletedMessage.class,getRef());
                Scheduler.subscribe(AssignmentCanceledMessage.class,getRef());
                Scheduler.subscribe(AssignmentStartedMessage.class,getRef());

                // The drone that should be chosen
                Drone correctDrone = createDrone(STERRE);
                // Add all the drones
                List<Drone> drones = new ArrayList<>();
                drones.add(correctDrone);
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(CITADELPARK));
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(VOORUIT));
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(GRAVENSTEEN));
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(ZUIDERPOORT));
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(PARIS));
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(LONDON));
                expectMsgClass(DroneAddedMessage.class);
                drones.add(createDrone(BERLIN));
                expectMsgClass(DroneAddedMessage.class);

                // Add the assignment
                Assignment assignment = new Assignment(Helper.routeTo(IKEAGENT),getUser());
                assignment.save();

                // Schedule!
                Scheduler.schedule();
                DroneAssignedMessage assignedMessage = expectMsgClass(DroneAssignedMessage.class);
                assignment.refresh();
                Assert.assertTrue("Correct assignment",assignedMessage.getAssignmentId() == assignment.getId());
                Assert.assertTrue("Assignment is scheduled", assignment.isScheduled());
                Assert.assertTrue("Closest drone", assignedMessage.getDroneId() == correctDrone.getId());
                Assert.assertTrue("Consistent database", assignment.getAssignedDrone().getId() == correctDrone.getId());

                // Assignment started
                AssignmentStartedMessage startedMessage = expectMsgClass(AssignmentStartedMessage.class);
                correctDrone.refresh();
                Assert.assertTrue("Assignment has started",assignment.getId() == assignedMessage.getAssignmentId());
                Assert.assertTrue("Drone status FLYING",correctDrone.getStatus() == Drone.Status.FLYING);

                // Create temp station for a quick return.
                Basestation tempStation = new Basestation("Ghent",STERRE);
                tempStation.save();

                // Cancel assignment
                Scheduler.cancelAssignment(assignment.getId());
                DroneUnassignedMessage unassignedMessage = expectMsgClass(DroneUnassignedMessage.class);
                correctDrone.refresh();
                assignment.refresh();
                Assert.assertTrue("Drone unassigned", unassignedMessage.getDroneId() == correctDrone.getId());
                Assert.assertTrue("Assignment unassigned", unassignedMessage.getAssignmentId() == assignment.getId());
                Assert.assertTrue("Assignment no assigned drone",assignment.getAssignedDrone() == null);
                AssignmentCanceledMessage canceledMessage = expectMsgClass(AssignmentCanceledMessage.class);
                assignment.refresh();
                Assert.assertTrue("Assignment canceled", canceledMessage.getAssignmentId() == assignment.getId());
                Assert.assertTrue("Assignment descheduled",!assignment.isScheduled());
                assignment.delete();

                // Wait a bit
                expectNoMsg(LONG_TIMEOUT);
                correctDrone.refresh();
                Assert.assertTrue("Drone available again",correctDrone.getStatus() == Drone.Status.AVAILABLE);


                // Remove test drones
                for(Drone drone : drones){
                    Scheduler.removeDrone(drone.getId());
                }
                // Receive drone removed messages
                receiveN(drones.size());
            }
        };
    }

    @Ignore
    @Test
    public void removeDrone_NotAssigned_Succeeds() throws SchedulerException{
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAddedMessage.class, getRef());
                Scheduler.subscribe(DroneRemovedMessage.class,getRef());
                // Add test drone
                Drone drone = createDrone(GHENT);
                DroneAddedMessage addedMessage = expectMsgClass(DroneAddedMessage.class);
                Assert.assertTrue(addedMessage.getDroneId() == drone.getId());
                drone.refresh();
                Assert.assertTrue(drone.getStatus() == Drone.Status.AVAILABLE);
                // Remove test drone
                Scheduler.removeDrone(drone.getId());
                DroneRemovedMessage removedMessage = expectMsgClass(DroneRemovedMessage.class);
                Assert.assertTrue(removedMessage.getDroneId() == drone.getId());
                drone.refresh();
                Assert.assertTrue(drone.getStatus() == Drone.Status.INACTIVE);
                drone.delete();
            }
        };
    }

    @Ignore
    @Test
    public void schedule_Complete_DroneReturns() throws SchedulerException{
        // TODO: waiting for FlightControl to complete this test
        new JavaTestKit(system){
            {
                Scheduler.subscribe(DroneAddedMessage.class, getRef());
                Scheduler.subscribe(DroneRemovedMessage.class,getRef());
                Scheduler.subscribe(DroneAssignedMessage.class,getRef());
                Scheduler.subscribe(AssignmentStartedMessage.class,getRef());
                Scheduler.subscribe(AssignmentCompletedMessage.class,getRef());

                // Add test drone in Ghent
                Drone drone = createDrone(GHENT);
                expectMsgClass(DroneAddedMessage.class);
                drone.refresh();
                Assert.assertTrue("Drone status AVAILABLE",drone.getStatus() == Drone.Status.AVAILABLE);

                // Add base station in Paris
                Basestation parisStation = new Basestation("Paris",PARIS);
                parisStation.save();

                // Add assignment in Ghent
                Assignment assignment = new Assignment(Helper.routeTo(GHENT),getUser());
                assignment.save();

                // Schedule the assignment and expect the usual messages.
                Scheduler.schedule();
                expectMsgClass(DroneAssignedMessage.class);
                expectMsgClass(AssignmentStartedMessage.class);
                // Assignment should complete immediately
                expectMsgClass(AssignmentCompletedMessage.class);
                assignment.refresh();
                Assert.assertTrue("Assignment unassigned",assignment.getAssignedDrone() == null);
                assignment.delete();

                // See if the drone is still returning to the Sydney base station
                expectNoMsg(SHORT_TIMEOUT);
                drone.refresh();
                Assert.assertTrue("Drone status FLYING",drone.getStatus() == Drone.Status.FLYING);

                // Delete Paris station, add Ghent station and removed drone.
                parisStation.delete();
                Basestation ghentStation = new Basestation("Ghent",GHENT);
                ghentStation.save();

                // Remove test drone with longer delay
                Scheduler.removeDrone(drone.getId());
                DroneRemovedMessage removedMessage = expectMsgClass(LONG_TIMEOUT,DroneRemovedMessage.class);
                drone.refresh();
                Assert.assertTrue("Drone status INACTIVE", drone.getStatus() == Drone.Status.INACTIVE);
                drone.delete();

                // Remove Ghent station
                ghentStation.delete();
            }
        };
    }

    private void subscribe(JavaTestKit test, Class<? extends SchedulerEvent> eventType) throws SchedulerException{
        Scheduler.subscribe(eventType, test.getRef());
        SubscribedMessage A = test.expectMsgClass(SubscribedMessage.class);
        Assert.assertTrue(A.getEventType() == eventType);
    }

    private void unsubscribe(JavaTestKit test, Class<? extends SchedulerEvent> eventType) throws SchedulerException{
        Scheduler.unsubscribe(eventType, test.getRef());
        UnsubscribedMessage A = test.expectMsgClass(UnsubscribedMessage.class);
        Assert.assertTrue(A.getEventType() == eventType);
    }

    public Assignment createAssignment(Location location) throws SchedulerException{
        Assignment assignment = new Assignment(Helper.routeTo(location),getUser());
        assignment.save();
        Scheduler.schedule();
        return assignment;
    }

    public Basestation createBasestation(String name, Location location) throws SchedulerException{
        Basestation basestation = new Basestation(name,location);
        basestation.save();
        return basestation;
    }

    public void addDrone(JavaTestKit test, Drone drone) throws SchedulerException{
        subscribe(test, DroneAddedMessage.class);
        Scheduler.addDrone(drone.getId());
        DroneAddedMessage A = test.expectMsgClass(DroneAddedMessage.class);
        drone.refresh();
        Assert.assertTrue("Drone added", A.getDroneId() == drone.getId());
        Assert.assertTrue("Drone AVAILABLE",drone.getStatus() == Drone.Status.AVAILABLE);
        unsubscribe(test, DroneAddedMessage.class);
    }

    public void removeDrone(JavaTestKit test, Drone drone) throws SchedulerException{
        subscribe(test, DroneRemovedMessage.class);
        Scheduler.addDrone(drone.getId());
        DroneRemovedMessage A = test.expectMsgClass(LONG_TIMEOUT,DroneRemovedMessage.class);
        drone.refresh();
        Assert.assertTrue("Drone removed", A.getDroneId() == drone.getId());
        Assert.assertTrue("Drone RETIRED",drone.getStatus() == Drone.Status.RETIRED);
        unsubscribe(test, DroneRemovedMessage.class);
    }

    public void assertCanceled(JavaTestKit test,Assignment assignment,Drone drone) throws SchedulerException{
        subscribe(test,DroneUnassignedMessage.class);
        subscribe(test, AssignmentCanceledMessage.class);
        Scheduler.cancelAssignment(assignment.getId());
        DroneUnassignedMessage A = test.expectMsgClass(DroneUnassignedMessage.class);
        assignment.refresh();
        drone.refresh();
        Assert.assertTrue("Assignment unassigned", A.getAssignmentId() == assignment.getId());
        Assert.assertTrue("Drone unassigned", A.getDroneId() == drone.getId());
        Assert.assertTrue("Assignment no drone",assignment.getAssignedDrone() == null);
        AssignmentCanceledMessage B = test.expectMsgClass(AssignmentCanceledMessage.class);
        assignment.refresh();
        Assert.assertTrue("Assignment canceled",B.getAssignmentId() == assignment.getId());
        Assert.assertTrue("Assignment unscheduled",!assignment.isScheduled());
        unsubscribe(test,DroneUnassignedMessage.class);
        unsubscribe(test, AssignmentCanceledMessage.class);
    }

    public void assertScheduled(JavaTestKit test, Assignment assignment, Drone drone) throws SchedulerException{
        subscribe(test, DroneAssignedMessage.class);
        subscribe(test,AssignmentStartedMessage.class);
        Scheduler.schedule();
        DroneAssignedMessage A = test.expectMsgClass(LONG_TIMEOUT, DroneAssignedMessage.class);
        assignment.refresh();
        drone.refresh();
        Assert.assertTrue("Assignment scheduled", assignment.isScheduled());
        Assert.assertTrue("Assignment assigned", A.getAssignmentId() == assignment.getId());
        Assert.assertTrue("Drone assigned", A.getDroneId() == drone.getId());
        Assert.assertTrue("Assignment drone", drone.equals(assignment.getAssignedDrone()));
        AssignmentStartedMessage B = test.expectMsgClass(AssignmentStartedMessage.class);
        assignment.refresh();
        drone.refresh();
        Assert.assertTrue("Assignment started",B.getAssignmentId() == assignment.getId());
        Assert.assertTrue("Drone FLYING",drone.getStatus() == Drone.Status.FLYING);
        unsubscribe(test, DroneAssignedMessage.class);
        unsubscribe(test, AssignmentStartedMessage.class);
    }

    @Ignore
    @Test
    public void schedule_cancelAssigment_succeeds() throws SchedulerException{
        new JavaTestKit(system){
            {
                createBasestation("Center",CENTER);
                Drone drone = createDrone(CENTER);
                addDrone(this,drone);

                Assignment assignment = null;
                for (int iteration = 0; iteration < 1; iteration++){
                    if(iteration % 2 == 0){
                        assignment = createAssignment(EAST);
                    }else{
                        assignment = createAssignment(WEST);
                    }
                    // Schedule
                    assertScheduled(this,assignment,drone);
                    assertCanceled(this,assignment,drone);
                    assignment.delete();
                }
                removeDrone(this,drone);
            }
        };
    }
}
