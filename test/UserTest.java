import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import org.junit.*;
import play.libs.Json;
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

        User user = new User("unauthorized.usercreation@user.tests.cros.com", "testtest", "Yasser", "Deceukelier");

        FakeRequest create = fakeRequest().withJsonBody(Json.toJson(user));

        Result result = callAction(routes.ref.UserController.createUser(), create);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void userCreation_ByAdmin_AddsUserToDB() {

        //User info
        String firstName = "Yasser";
        String lastName = "Deceukelier";
        String email = "unauthorized.usercreation@user.tests.cros.com";
        String password = "testtest";

        // Create json representation
        ObjectMapper jsonMapper = new ObjectMapper();

        ObjectNode userNode = jsonMapper.createObjectNode();
        userNode.put("firstName", firstName);
        userNode.put("lastName", lastName);
        userNode.put("email", email);
        userNode.put("password", password);

        ObjectNode root = jsonMapper.createObjectNode();
        root.put("User", userNode);

        FakeRequest create = fakeRequest().withJsonBody(root);

        Result result = callAction(routes.ref.UserController.createUser(), authorizeRequest(create, getAdmin()));
        System.err.println(contentAsString(result));
        assertThat(status(result)).isEqualTo(CREATED);

        User createdUser = User.findByEmail(email);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.firstName).isEqualTo(firstName);
        assertThat(createdUser.lastName).isEqualTo(lastName);
        assertThat(createdUser.checkPassword(password)).isTrue();

        createdUser.delete();
    }

    @Test
    public void updateUser_ByUnpriviledgedUser_ReturnsUnauthorized() {

        // Create original user
        String firstName = "John";
        String lastName = "Doe";
        String email = "unauthorized.userupdate@user.tests.cros.com";

        User u = new User(email, "password", firstName, lastName);
        u.save();

        // Send request to update this user
        String newFirstName = "Jane";
        u.firstName = newFirstName;
        JsonNode data = Json.toJson(u);

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
        String firstName = "John";
        String lastName = "Doe";
        String email = "admin.userupdate@user.tests.cros.com";

        User u = new User(email, "password", firstName, lastName);
        u.save();

        // Send request to update this user
        String newFirstName = "Jane";
        u.firstName = newFirstName;
        JsonNode data = Json.toJson(u);

        Result result = updateUser(u.id, data, getAdmin());
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        u = User.find.byId(u.id);
        assertThat(u.firstName).isEqualTo(newFirstName);

        u.delete();
    }

    @Test
    public void updateUser_ByUserHimself_UpdatesDBEntry() {

        // Create original user
        String firstName = "John";
        String lastName = "Doe";
        String email = "himself.userupdate@user.tests.cros.com";

        User u = new User(email, "password", firstName, lastName);
        u.save();

        // Send request to update this user
        String newFirstName = "Jane";
        u.firstName = newFirstName;
        JsonNode data = Json.toJson(u);

        Result result = updateUser(u.id, data, u);
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        u = User.find.byId(u.id);
        assertThat(u.firstName).isEqualTo(newFirstName);

        u.delete();
    }

    private Result updateUser(Long id, JsonNode data, User requester) {
        FakeRequest update = fakeRequest().withJsonBody(data);
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
