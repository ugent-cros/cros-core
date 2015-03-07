import models.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;
import controllers.routes;


import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

/**
 * Created by yasser on 4/03/15.
 */
public class UserTest extends TestSuperclass {

    // Code to provide fake app

    private static FakeApplication app;

    @BeforeClass
    public static void setup() {
        startFakeApplication();
    }

    @AfterClass
    public static void tearDown() {
        stopFakeApplication();
    }

    @Ignore
    @Test
    public void checkPasswordHash() {

        String password = "lolcats";
        User u = new User("student@ugent.be", password, "test", "student");
        assertThat(u.checkPassword(password)).isTrue();
        assertThat(u.checkPassword("lol")).isFalse();
    }

    @Test
    public void rolesTest() {

        User u = new User("student@ugent.be", "password", "test", "student");
        u.addRole(User.UserRole.ADMIN);
        System.err.println(u.getRoles());
        u.save();
        System.err.println(u.getRoles());

        u = User.find.byId(u.id);
        System.err.println(u.getRoles());
        assertThat(u.hasRole(User.UserRole.ADMIN)).isTrue();
    }

    @Ignore
    @Test
    public void checkUserCreation() {

        Map<String, String> data = new HashMap<>();
        data.put("email", "yasser.deceukelier@ugent.be");
        data.put("password", "test");
        data.put("firstName", "Yasser");
        data.put("lastName", "Deceukelier");

        FakeRequest createUser = authorizedRequest.withFormUrlEncodedBody(data);
        Result result = callAction(routes.ref.UserController.createUser(), createUser);

        System.out.println(contentAsString(result));
        assertThat(status(result)).isEqualTo(CREATED);
    }
}
