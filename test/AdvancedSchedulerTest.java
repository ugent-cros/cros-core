import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.scheduler.AdvancedScheduler;
import drones.models.scheduler.Helper;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.messages.from.SchedulerReplyMessage;
import drones.models.scheduler.messages.to.SchedulerRequestMessage;
import models.Assignment;
import models.Basestation;
import models.Checkpoint;
import models.Location;
import org.junit.*;
import play.libs.Akka;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ronald on 18/04/2015.
 */
public class AdvancedSchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    // Our location
    private static final double DIST_GHENT_ANTWERP = 51474;
    private static final double DIST_ANTWERP_PARIS = 301065;
    private static final Location GHENT = new Location(3.71667,51.05,0);
    private static final Location ANTWERP = new Location(4.40346,51.21989,0);
    private static final Location PARIS = new Location(2.3488,48.85341,0);
    // Other basestations
    private static final List<Basestation> BASESTATIONS = new ArrayList<>();
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
        Ebean.save(BASESTATIONS);
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
    public void subscriberTest_RequestMessage_ReplyMessage(){
        new JavaTestKit(system){
            {
                try {
                    Scheduler.subscribe(SchedulerReplyMessage.class, getRef());
                    ActorRef scheduler = Scheduler.getScheduler();
                    SchedulerRequestMessage request = new SchedulerRequestMessage();
                    scheduler.tell(request,getRef());
                    SchedulerReplyMessage reply = expectMsgClass(SchedulerReplyMessage.class);
                    Assert.assertTrue(request.getRequestId() == reply.getRequestId());
                }catch(Exception ex){
                    Assert.fail(ex.getMessage());
                }
            }
        };
    }


}
