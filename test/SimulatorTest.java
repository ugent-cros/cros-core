import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import drones.messages.*;
import drones.models.DroneCommander;
import drones.models.FlyingState;
import drones.simulation.BepopSimulator;
import drones.simulation.SimulatorDriver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by yasser on 13/04/15.
 */
public class SimulatorTest extends TestSuperclass {

    private SimulatorDriver driver = new SimulatorDriver();
    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        stopFakeApplication();
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }


    protected TestActorRef<BepopSimulator> newSimulator() {
        Props props = Props.create(driver.getActorClass(),
                () -> driver.createActor(""));
        return TestActorRef.create(system, props);
    }

    protected DroneCommander newCommander() {
        return new DroneCommander("", driver);
    }

    @Test
    public void init_AllOK_Success() {

        Future<Void> f = newCommander().init();
        assertThat(f.isCompleted());
    }

    @Test
    public void takeOff_Initialized_TakesOff() {

        new JavaTestKit(system) {{

            // Prepare commander
            DroneCommander commander = newCommander();
            commander.init();

            // Setup listener
            JavaTestKit listener = new JavaTestKit(system);
            commander.subscribeTopic(listener.getRef(), FlyingStateChangedMessage.class);
            commander.subscribeTopic(listener.getRef(), AltitudeChangedMessage.class);

            // Take-off
            commander.takeOff();

            // Check if expected messages are received
            FlyingStateChangedMessage stateMsg = listener.expectMsgClass(FlyingStateChangedMessage.class);
            assertThat(stateMsg.getState()).isEqualTo(FlyingState.TAKINGOFF);

            AltitudeChangedMessage altitudeMsg = listener.expectMsgClass(AltitudeChangedMessage.class);
            assertThat(altitudeMsg.getAltitude()).isEqualTo(1.0);

            stateMsg = listener.expectMsgClass(FlyingStateChangedMessage.class);
            assertThat(stateMsg.getState()).isEqualTo(FlyingState.HOVERING);
        }};
    }

    @Test
    public void land_Hovering_Lands() throws Exception {

        new JavaTestKit(system) {{

            // Prepare commander
            DroneCommander commander = newCommander();
            commander.init();

            // Setup listener
            JavaTestKit listener = new JavaTestKit(system);
            commander.subscribeTopic(listener.getRef(), FlyingStateChangedMessage.class);
            commander.subscribeTopic(listener.getRef(), AltitudeChangedMessage.class);

            FlyingStateChangedMessage flyingState = null;
            AltitudeChangedMessage altitude = null;

            // Take-off
            commander.takeOff();
            // verify messages to be sure drone has taken off
            listener.expectMsgClass(FlyingStateChangedMessage.class);   // TAKING-OFF
            listener.expectMsgClass(AltitudeChangedMessage.class);      // altidude = 1
            listener.expectMsgClass(FlyingStateChangedMessage.class);   // HOVERING

            // Land
            commander.land();

            // Check if expected messages are received
            flyingState = listener.expectMsgClass(FlyingStateChangedMessage.class);
            assertThat(flyingState.getState()).isEqualTo(FlyingState.LANDING);

            altitude = listener.expectMsgClass(AltitudeChangedMessage.class);
            assertThat(altitude.getAltitude()).isEqualTo(0);

            flyingState = listener.expectMsgClass(FlyingStateChangedMessage.class);
            assertThat(flyingState.getState()).isEqualTo(FlyingState.LANDED);
        }};
    }

    @Test
    public void move3dNorth_Hovering_Moves() throws Exception {

        // Prepare commander
        DroneCommander commander = newCommander();
        commander.init();
        commander.takeOff();

        // Move forward
        assertMovement(commander, 1, 0);
        // Move backward
        assertMovement(commander, -1, 0);
        // Move left
        assertMovement(commander, 0, 1);
        // Move right
        assertMovement(commander, 0, -1);

    }

    private void assertMovement(DroneCommander commander, double vx, double vy) {

        new JavaTestKit(system) {{

            // Setup listener
            JavaTestKit listener = new JavaTestKit(system);
            JavaTestKit tracker = new JavaTestKit(system);
            commander.subscribeTopic(listener.getRef(), AttitudeChangedMessage.class);
            commander.subscribeTopic(listener.getRef(), SpeedChangedMessage.class);
            commander.subscribeTopic(tracker.getRef(), LocationChangedMessage.class);

            AttitudeChangedMessage rotation = null;
            SpeedChangedMessage speed = null;
            LocationChangedMessage initialLocation = tracker.expectMsgClass(LocationChangedMessage.class);

            // Move
            commander.move3d(vx, vy, 0, 0);

            // Check if rotation & speed is updated appropriatly
            rotation = listener.expectMsgClass(Duration.create(5, TimeUnit.SECONDS), AttitudeChangedMessage.class);
            assertThat(rotation.getPitch()).isEqualTo(vx * Math.PI/3);
            assertThat(rotation.getRoll()).isEqualTo(vy * Math.PI/3);

            speed = listener.expectMsgClass(SpeedChangedMessage.class);
            // Check speedX
            if (vx > 0) {
                assertThat(speed.getSpeedX()).isGreaterThan(0);
            } else if (vx < 0) {
                assertThat(speed.getSpeedX()).isLessThan(0);
            } else {
                assertThat(speed.getSpeedX() == 0);
            }
            // Check speedY
            if (vy > 0) {
                assertThat(speed.getSpeedY()).isGreaterThan(0);
            } else if (vy < 0) {
                assertThat(speed.getSpeedY()).isLessThan(0);
            } else {
                assertThat(speed.getSpeedY() == 0);
            }

            // Check if location changed correctly
            new JavaTestKit.AwaitAssert(Duration.create(3, TimeUnit.SECONDS), Duration.create(1, TimeUnit.SECONDS)) {
                @Override
                protected void check() {

                    // !!! this check won't work when crossing the 180° longitude or 90° latitude

                    LocationChangedMessage location = tracker.expectMsgClass(
                            Duration.create(2, TimeUnit.SECONDS),
                            LocationChangedMessage.class);

                    if (vx > 0) {   // Going north
                        assertThat(location.getLatitude()).isGreaterThan(initialLocation.getLatitude());
                    } else if (vx < 0) {    // Going south
                        assertThat(location.getLatitude()).isLessThan(initialLocation.getLatitude());
                    } else {
                        assertThat(location.getLatitude()).isEqualTo(initialLocation.getLatitude());
                    }

                    if (vy > 0) {   // Going east
                        assertThat(location.getLongitude()).isGreaterThan(initialLocation.getLongitude());
                    } else if (vy < 0) {    // Going west
                        assertThat(location.getLongitude()).isLessThan(initialLocation.getLongitude());
                    } else {
                        assertThat(location.getLongitude()).isEqualTo(initialLocation.getLongitude());
                    }

                }
            };

            // Check if rotation and speed go back to hovering by default
            rotation = listener.expectMsgClass(AttitudeChangedMessage.class);
            assertThat(rotation.getRoll()).isEqualTo(0);
            assertThat(rotation.getYaw()).isEqualTo(0);

            speed = listener.expectMsgClass(SpeedChangedMessage.class);
            assertThat(speed.getSpeedX()).isEqualTo(0);
            assertThat(speed.getSpeedY()).isEqualTo(0);

            // Unsubscribe
            commander.unsubscribe(listener.getRef());
            commander.unsubscribe(tracker.getRef());
        }};
    }
}
