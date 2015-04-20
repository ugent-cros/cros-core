import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Created by Sander on 13/04/2015.
 */
public class SimpleControlTowerTest extends TestSuperclass{

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        system = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
        stopFakeApplication();
    }
}
