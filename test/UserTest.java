import models.User;
import org.junit.*;
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

    @BeforeClass
    public static void setup() {
        startFakeApplication();
    }

    @AfterClass
    public static void tearDown() {
        stopFakeApplication();
    }

    @Test
    public void checkPassword_PasswordIsCorrect_ReturnsTrue() {

        String password = "lolcats1";
        User u = new User("student@ugent.be", password, "test", "student");
        assertThat(u.checkPassword(password)).isTrue();
    }

    @Test
    public void checkPassword_PasswordIsWrong_ReturnsFalse() {

        String password = "lolcats1";
        User u = new User("student@ugent.be", password, "test", "student");
        assertThat(u.checkPassword("lol")).isFalse();
    }


    @Test
    public void userCreation_ByUnpriviledgedUser_ReturnsUnauthorized() {

        Map<String, String> data = new HashMap<>();
        data.put("email", "yasser.deceukelier@ugent.be");
        data.put("password", "testtest");
        data.put("firstName", "Yasser");
        data.put("lastName", "Deceukelier");

        FakeRequest create = fakeRequest().withFormUrlEncodedBody(data);

        Result result = callAction(routes.ref.UserController.createUser(), create);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getAdmin()));
        assertThat(status(result)).isEqualTo(CREATED);

        System.out.println(contentAsString(result));
    }

    @Test
    public void userCreation_ByAdmin_ReturnsCreated() {

        Map<String, String> data = new HashMap<>();
        data.put("email", "yasser.deceukelier@ugent.be");
        data.put("password", "testtest");
        data.put("firstName", "Yasser");
        data.put("lastName", "Deceukelier");

        FakeRequest create = fakeRequest().withFormUrlEncodedBody(data);

        Result result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getAdmin()));
        assertThat(status(result)).isEqualTo(CREATED);

        System.out.println(contentAsString(result));
    }
}
