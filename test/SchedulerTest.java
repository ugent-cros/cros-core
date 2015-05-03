import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import drones.scheduler.AdvancedScheduler;
import drones.scheduler.Scheduler;
import drones.scheduler.SchedulerException;
import drones.scheduler.messages.from.SchedulerStoppedMessage;
import drones.scheduler.messages.from.SubscribedMessage;
import drones.scheduler.messages.from.UnsubscribedMessage;
import org.junit.*;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 6/04/2015.
 */
public class SchedulerTest extends TestSuperclass {

    private static ActorSystem system;
    private static final Duration timeout = Duration.create(3000,TimeUnit.MILLISECONDS);

    @BeforeClass
    public static void createSystem() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void destroySystem() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void before() {
        startFakeApplication();
    }

    @After
    public void after() {
        stopFakeApplication();
    }

    @Test
    public void getScheduler_Started_Succeeds(){
        Scheduler.getScheduler();
    }

    @Test
    public void stop_Started_Succeeds(){
        Scheduler.stop();
    }

    @Test(expected = SchedulerException.class)
    public void start_Started_Fails(){
        Scheduler.start(AdvancedScheduler.class);
    }

    @Test
    public void start_NotStarted_Succeeds(){
        new JavaTestKit(system){
            {
                ActorRef scheduler = Scheduler.getScheduler();
                watch(scheduler);
                Scheduler.subscribe(SchedulerStoppedMessage.class, getRef());
                expectMsgClass(SubscribedMessage.class);
                Scheduler.stop();
                expectMsgClass(SchedulerStoppedMessage.class);
                expectTerminated(scheduler);
                Scheduler.start(AdvancedScheduler.class);
                Scheduler.unsubscribe(SchedulerStoppedMessage.class, getRef());
                expectMsgClass(UnsubscribedMessage.class);
            }
        };
    }

    @Test(expected = SchedulerException.class)
    public void getScheduler_NotStarted_Fails() {
        new JavaTestKit(system){
            {
                ActorRef scheduler = Scheduler.getScheduler();
                watch(scheduler);
                Scheduler.stop();
                expectTerminated(scheduler);
                Scheduler.getScheduler();
            }
        };
    }

    @Test(expected = SchedulerException.class)
    public void stop_NotStarted_Fails() {
        new JavaTestKit(system){
            {
                ActorRef scheduler = Scheduler.getScheduler();
                watch(scheduler);
                Scheduler.stop();
                expectTerminated(scheduler);
                Scheduler.stop();
            }
        };
    }
}
