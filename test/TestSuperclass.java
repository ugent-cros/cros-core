import controllers.SecurityController;
import drones.simulation.SimulatorDriver;
import models.User;
import play.test.*;

import static play.test.Helpers.*;

/**
 * Created by Benjamin on 4/03/2015.
 */
public class TestSuperclass {

    public static FakeApplication application;

    private static User admin;
    public static User getAdmin() { return admin; }

    private static User roAdmin;
    public static User getReadOnlyAdmin() { return roAdmin; }

    private static User user;
    public static User getUser() { return user; }
    protected boolean setup = false;
    protected static SimulatorDriver driver = new SimulatorDriver();

    public static void startFakeApplication() {
        application = fakeApplication(inMemoryDatabase());
        start(application);

        // Create a user of each role to test with
        admin = new User("admin@test.cros", "password", "Admin", "Tester");
        admin.setRole(User.Role.ADMIN);
        admin.save();
        roAdmin = new User("ro-admin@test.cros", "password", "Readonly-Admin", "Tester");
        roAdmin.setRole(User.Role.READONLY_ADMIN);
        roAdmin.save();
        user = new User("user@test.cros", "password", "User", "Tester");
        user.setRole(User.Role.USER);
        user.save();
    }

    public static void stopFakeApplication() {
        stop(application);
    }

    public static FakeRequest authorizeRequest(FakeRequest request, User user) {
        return request.withHeader(SecurityController.AUTH_TOKEN_HEADER, user.getAuthToken());
    }
}
