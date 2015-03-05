import models.User;
import org.junit.*;

import play.test.*;

import static play.test.Helpers.*;

/**
 * Created by Benjamin on 4/03/2015.
 */
public class TestSuperclass {

    public static FakeApplication application;
    public static FakeRequest authorizedRequest;

    public static void startFakeApplication() {
        application = fakeApplication(inMemoryDatabase());
        start(application);

        // Create and store user in in memory database
        User authenticatedUser = new User("john.doe@test.com", "testing", "John Doe");
        authenticatedUser.save();

        // Create an (un)authorized request for later test
        String token = authenticatedUser.createToken();
        long id = authenticatedUser.id;
        authorizedRequest = fakeRequest().withHeader("X-AUTH-TOKEN", token).withHeader("X-AUTH-ID", "" + id);
    }

    public static void stopFakeApplication() {
        stop(application);
    }
}
