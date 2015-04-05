import akka.actor.ActorSystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
//import akka.testkit.JavaTestKit;

/**
 * Created by Sander on 5/04/2015.
 */
public class SimplePilotTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        //JavaTestKit.shutdownActorSystem(system);
        system = null;
    }
}
