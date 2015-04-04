import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.UnitPFBuilder;
import akka.pattern.Patterns;
import drones.messages.*;
import drones.models.*;
import org.junit.*;
import play.libs.Akka;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Created by Cedric on 4/2/2015.
 */
public class DroneActorTest extends TestSuperclass{


    private static final Duration TIMEOUT = Duration.create(1, TimeUnit.SECONDS);

    private ActorRef droneActor;
    private DroneCommander commander;

    @BeforeClass
    public static void setup() {
        startFakeApplication();
    }

    @AfterClass
    public static void tearDown() {
        stopFakeApplication();
    }

    @Before
    public void init() {
        droneActor = Akka.system().actorOf(
                Props.create(TestDroneActor.class));
        commander = new DroneCommander(droneActor);
    }

    @Test
    public void setLocationMessage_Arrives() throws Exception {
        droneActor.tell(new LocationChangedMessage(1, 2, 3), null);
        Location location = Await.result(commander.getLocation(), TIMEOUT);
        Assert.assertEquals(location.getLatitude(), 2, 0.0d);
    }

    @Test
    public void setGPSFix_Arrives() throws Exception {
        droneActor.tell(new GPSFixChangedMessage(true), null);
        boolean res = Await.result(commander.isGPSFixed(), TIMEOUT);
        Assert.assertTrue(res);

        droneActor.tell(new GPSFixChangedMessage(false), null);
        res = Await.result(commander.isGPSFixed(), TIMEOUT);
        Assert.assertFalse(res);
    }

    @Test
    public void batteryPercentage_Arrives() throws Exception {
        droneActor.tell(new BatteryPercentageChangedMessage((byte)25), null);
        byte res = Await.result(commander.getBatteryPercentage(), TIMEOUT);
        Assert.assertEquals(res, (byte) 25);
    }

    @Test
    public void flyingState_Arrives() throws Exception {
        droneActor.tell(new FlyingStateChangedMessage(FlyingState.HOVERING), null);
        FlyingState res = Await.result(commander.getFlyingState(), TIMEOUT);
        Assert.assertEquals(res, FlyingState.HOVERING);
    }

    @Test
    public void attitude_Arrives() throws Exception {
        droneActor.tell(new AttitudeChangedMessage(1, 2, 42), null);
        Rotation res = Await.result(commander.getRotation(), TIMEOUT);
        Assert.assertEquals(res.getRoll(), 1, 0);
        Assert.assertEquals(res.getPitch(), 2, 0);
        Assert.assertEquals(res.getYaw(), 42, 0);
    }

    @Test
    public void altitude_Arrives() throws Exception {
        droneActor.tell(new AltitudeChangedMessage(42f), null);
        double res = Await.result(commander.getAltitude(), TIMEOUT);
        Assert.assertEquals(res, 42, 0);
    }

    @Test
    public void speedchanged_Arrives() throws Exception {
        droneActor.tell(new SpeedChangedMessage(1f, 2f, 3f), null);
        Speed res = Await.result(commander.getSpeed(), TIMEOUT);
        Assert.assertEquals(res.getVx(), 1f, 0);
        Assert.assertEquals(res.getVy(), 2f, 0);
        Assert.assertEquals(res.getVz(), 3f, 0);
    }

    @Test
    public void navigationStateChanged_Arrives() throws Exception {
        droneActor.tell(new NavigationStateChangedMessage(NavigationState.PENDING, NavigationStateReason.REQUESTED), null);
        NavigationState state = Await.result(commander.getNavigationState(), TIMEOUT);
        NavigationStateReason reason = Await.result(commander.getNavigationStateReason(), TIMEOUT);
        Assert.assertEquals(state, NavigationState.PENDING);
        Assert.assertEquals(reason, NavigationStateReason.REQUESTED);
    }


    @Test
    public void connectionStatus_Arrives() throws Exception {
        droneActor.tell(new ConnectionStatusChangedMessage(false), null);
        boolean online = Await.result(commander.isOnline(), TIMEOUT);
        Assert.assertFalse(online);
        droneActor.tell(new ConnectionStatusChangedMessage(true), null);
        online = Await.result(commander.isOnline(), TIMEOUT);
        Assert.assertTrue(online);
    }

    @Test
    public void subscribe_receives() throws Exception {
        ActorRef sub = Akka.system().actorOf(
                Props.create(FakeSubscriber.class));
        commander.subscribeTopic(sub, GPSFixChangedMessage.class);

        droneActor.tell(new GPSFixChangedMessage(true), null);
        Thread.sleep(500); //TODO: Allow publisher to publish with some sort of callback
        Future<Object> obj = Patterns.ask(sub, new FakeSubscriber.LastMessageRequest(), 2000);
        FakeSubscriber.LastMessageResponse msg = (FakeSubscriber.LastMessageResponse)Await.result(obj, TIMEOUT);
        Assert.assertTrue(((GPSFixChangedMessage) msg.getValue()).isFixed());

        droneActor.tell(new LocationChangedMessage(0, 0, 0), null);
        Thread.sleep(500);
        obj = Patterns.ask(sub, new FakeSubscriber.LastMessageRequest(), 2000);
        FakeSubscriber.LastMessageResponse res = (FakeSubscriber.LastMessageResponse)Await.result(obj, TIMEOUT);
        Assert.assertNull(res.getValue());

        commander.unsubscribeTopic(sub, GPSFixChangedMessage.class);
        droneActor.tell(new GPSFixChangedMessage(true), null);
        Thread.sleep(500); //TODO: Allow publisher to publish with some sort of callback
        obj = Patterns.ask(sub, new FakeSubscriber.LastMessageRequest(), 2000);
        res = (FakeSubscriber.LastMessageResponse)Await.result(obj, TIMEOUT);
        Assert.assertNull(res.getValue());

        // Cleanup
        sub.tell(new StopMessage(), null);
    }

    @Test
    public void flattrim_Arrives() throws InterruptedException, TimeoutException {
        Await.ready(commander.flatTrim(), TIMEOUT); //will throw when failed
    }

    @Test
    public void setHull_Arrives() throws InterruptedException, TimeoutException {
        Await.ready(commander.setHull(false), TIMEOUT); //will throw when failed
    }

    @Test
    public void setOutdoor_Arrives() throws InterruptedException, TimeoutException {
        Await.ready(commander.setOutdoor(false), TIMEOUT); //will throw when failed
    }

    @Test
    public void calibrate_Arrives() throws InterruptedException, TimeoutException {
        Await.ready(commander.calibrate(false, false), TIMEOUT); //will throw when failed
    }

}
