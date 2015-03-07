import com.avaje.ebean.Ebean;
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

    // email format: test.name@user.tests.cros.com

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
        User u = new User("correct.password@user.tests.cros.com", password, "test", "student");
        assertThat(u.checkPassword(password)).isTrue();
    }

    @Test
    public void checkPassword_PasswordIsWrong_ReturnsFalse() {

        String password = "lolcats1";
        User u = new User("wrong.password@user.tests.cros.com", password, "test", "student");
        assertThat(u.checkPassword("lol")).isFalse();
    }


    @Test
    public void userCreation_ByUnpriviledgedUser_ReturnsUnauthorized() {

        Map<String, String> data = new HashMap<>();
        data.put("email", "unauthorized.usercreation@user.tests.cros.com");
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
    }

    @Test
    public void userCreation_ByAdmin_AddsUserToDB() {

        Map<String, String> data = new HashMap<>();
        String email = "authorized.usercreation@user.tests.cros.com";
        String firstName = "Yasser";
        String lastName = "Deceukelier";
        data.put("email", email);
        data.put("password", "testtest");
        data.put("firstName", firstName);
        data.put("lastName", lastName);

        FakeRequest create = fakeRequest().withFormUrlEncodedBody(data);

        Result result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getAdmin()));
        assertThat(status(result)).isEqualTo(CREATED);

        User createdUser = User.findByEmail(email);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.firstName).isEqualTo(firstName);
        assertThat(createdUser.lastName).isEqualTo(lastName);

        createdUser.delete();
    }

    @Test
    public void updateUser_ByUnpriviledgedUser_ReturnsUnauthorized() {

        // Create original user
        User u = new User("unauthorized.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user
        Map<String, String> data = new HashMap<>();
        data.put("firstName", "Jane");

        FakeRequest update = fakeRequest().withFormUrlEncodedBody(data);

        Result result = updateUser(u.id, data, null);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = updateUser(u.id, data, getUser());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = updateUser(u.id, data, getReadOnlyAdmin());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void updateUser_ByAdmin_UpdatesDBEntry() {

        // Create original user
        User u = new User("admin.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user as admin
        Map<String, String> data = new HashMap<>();
        String newName = "Jane";
        data.put("firstName", newName);

        Result result = updateUser(u.id, data, getAdmin());
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        u = User.find.byId(u.id);
        assertThat(u.firstName).isEqualTo(newName);

        u.delete();
    }

    @Test
    public void updateUser_ByUserHimself_UpdatesDBEntry() {

        // Create original user
        User u = new User("owner.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user as admin
        Map<String, String> data = new HashMap<>();
        String newName = "Jane";
        data.put("firstName", newName);

        Result result = updateUser(u.id, data, u);
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        u = User.find.byId(u.id);
        assertThat(u.firstName).isEqualTo(newName);

        u.delete();
    }

    private Result updateUser(Long id, Map<String, String> data, User requester) {
        FakeRequest update = fakeRequest().withFormUrlEncodedBody(data);
        if(requester != null) {
            update = authorizeRequest(update, requester);
        }
        Result result = callAction(routes.ref.UserController.updateUser(id), update);
        return result;
    }

    @Ignore
    @Test
    public void allUsers_ByUnpriviledgedUser_ReturnsUnauthorized() {

        Result result = callAction(routes.ref.UserController.allUsers(), fakeRequest());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.allUsers(), authorizeRequest(fakeRequest(), getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Ignore
    @Test
    public void allUsers_ByPriviledgedUser_ReturnsListOfUsers() {

        Result result = callAction(routes.ref.UserController.allUsers(), authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        result = callAction(routes.ref.UserController.allUsers(), authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        // TODO: compare with list given by User class
    }
}
