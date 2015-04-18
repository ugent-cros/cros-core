import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.avaje.ebean.Ebean;
import drones.models.scheduler.AdvancedScheduler;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.messages.from.SchedulerReplyMessage;
import drones.models.scheduler.messages.to.SchedulerRequestMessage;
import models.Basestation;
import models.Location;
import org.junit.Assert;
import org.junit.Test;
import play.libs.Akka;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ronald on 18/04/2015.
 */
public class AdvancedSchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    // Our location
    private static final Location GHENT = new Location(3.71667,51.05,0);
    // Other basestations
    private static final List<Basestation> BASESTATIONS = new ArrayList<>();
    private static final Basestation BRUSSELS = new Basestation("Brussels", new Location(4.34878,50.85045,0));
    private static final Basestation DELHI = new Basestation("Delhi", new Location(77.22897,28.65381,0));
    private static final Basestation KINSHASA = new Basestation("Kinshasa", new Location(15.31357,-4.32758,0));
    private static final Basestation LIMA = new Basestation("Lima", new Location(-77.02824,-12.04318,0));
    private static final Basestation MOSCOW = new Basestation("Moscow", new Location(37.61556,55.75222,0));
    private static final Basestation NEW_YORK = new Basestation("New York", new Location(-74.00597,40.71427,0));
    private static final Basestation ROME = new Basestation("Rome", new Location(12.51133,41.89193,0));
    private static final Basestation SEATTLE = new Basestation("Seattle", new Location(-122.33207,47.60621,0));
    private static final Basestation SYDNEY = new Basestation("Sydney", new Location(151.20732,-33.86785,0));
    private static final Basestation TOKYO = new Basestation("Tokyo", new Location(139.69171,35.6895,0));

    public static void setup(){
        startFakeApplication();
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

    public static void tearDown(){
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
    }

    @Test
    public void closestBaseStation_Ghent_Brussels(){
        Basestation closest = AdvancedScheduler.closestBaseStation(GHENT);
        Assert.assertTrue(closest.equals(BRUSSELS));
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
