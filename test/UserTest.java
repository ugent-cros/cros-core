import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import controllers.routes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import utilities.JsonHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;
import static utilities.JsonHelper.removeRootElement;

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
    public void checkPassword_PasswordIsCorrect_TrueReturned() {
        String password = "lolcats1";
        User u = new User("correct.password@user.tests.cros.com", password, "test", "student");
        assertThat(u.checkPassword(password)).isTrue();
    }

    @Test
    public void checkPassword_PasswordIsWrong_FalseReturned() {
        String password = "lolcats1";
        User u = new User("wrong.password@user.tests.cros.com", password, "test", "student");
        assertThat(u.checkPassword("lol")).isFalse();
    }


    @Test
    public void create_UnauthorizedRequest_UnauthorizedReturned() {
        User user = new User("unauthorized.usercreation@user.tests.cros.com", "testtest", "Yasser", "Deceukelier");

        FakeRequest create = fakeRequest().withJsonBody(Json.toJson(user));

        Result result = callAction(routes.ref.UserController.create(), create);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.create(), authorizeRequest(create, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.create(), authorizeRequest(create, getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void create_AuthorizedInvalidRequest_BadRequestReturned() {
        String email = "unauthorized.usercreation@user.tests.cros.com";
        User u = new User(email,"testtest","Yasser","Deceukelier");
        ObjectNode objectNode = (ObjectNode) Json.toJson(u);

        // Create json representation
        JsonNode node = JsonHelper.addRootElement(objectNode, User.class);

        JsonNode empty = Json.toJson("lol");
        FakeRequest create = fakeRequest().withJsonBody(empty);

        Result result = callAction(routes.ref.UserController.create(), authorizeRequest(create, getAdmin()));
        assertThat(status(result)).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void create_AuthorizedRequest_UserCreated() {
        String email = "unauthorized.usercreation@user.tests.cros.com";
        User u = new User(email,"testtest","Yasser","Deceukelier");
        ObjectNode objectNode = (ObjectNode) Json.toJson(u);
        objectNode.put("password", "testtest");

        // Create json representation
        JsonNode node = JsonHelper.addRootElement(objectNode, User.class);

        FakeRequest create = fakeRequest().withJsonBody(node);

        Result result = callAction(routes.ref.UserController.create(), authorizeRequest(create, getAdmin()));
        assertThat(status(result)).isEqualTo(CREATED);

        User receivedUser =
                Json.fromJson(removeRootElement(contentAsString(result), User.class),User.class);
        u.setId(receivedUser.getId()); // bypass id because u has no id yet
        assertThat(u).isEqualTo(receivedUser);

        User createdUser = User.findByEmail(email);
        assertThat(u).isEqualTo(createdUser);

        createdUser.delete();
    }

    @Test
    public void update_UnauthorizedRequest_UnauthorizedReturned() {

        // Create original user
        String firstName = "John";
        String lastName = "Doe";
        String email = "unauthorized.userupdate@user.tests.cros.com";

        User u = new User(email, "password", firstName, lastName);
        u.save();

        // Send request to update this user
        String newFirstName = "Jane";
        u.setFirstName(newFirstName);
        JsonNode data = Json.toJson(u);

        Result result = updateUser(u.getId(), data, null);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = updateUser(u.getId(), data, getUser());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = updateUser(u.getId(), data, getReadOnlyAdmin());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void update_AuthorizedInvalidRequest_BadRequestReturned() {
        User u = new User("admin.invaliduserupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        JsonNode emptyNode = Json.toJson("invalid");

        FakeRequest update = fakeRequest().withJsonBody(emptyNode);
        Result result = callAction(routes.ref.UserController.update(u.getId()), authorizeRequest(update, getAdmin()));
        assertThat(status(result)).isEqualTo(BAD_REQUEST);
    }

    @Test
    public void update_AuthorizedRequest_UserUpdated() {
        User u = new User("admin.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user
        u.setFirstName("Jane");
        JsonNode data = JsonHelper.addRootElement(Json.toJson(u), User.class);

        Result result = updateUser(u.getId(), data, getAdmin());
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        User receivedUser =
                Json.fromJson(removeRootElement(contentAsString(result), User.class), User.class);
        assertThat(receivedUser).isEqualTo(u);
        User fetchedUser = User.FIND.byId(u.getId());
        assertThat(fetchedUser).isEqualTo(u);
    }

    @Test
    public void update_AuthorizedRequestByUser_UserUpdated() {
        User u = new User("himself.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user
        u.setFirstName("Jane");
        JsonNode data = JsonHelper.addRootElement(Json.toJson(u), User.class);

        Result result = updateUser(u.getId(), data, u);
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        User receivedUser =
                Json.fromJson(removeRootElement(contentAsString(result), User.class), User.class);
        assertThat(receivedUser).isEqualTo(u);
        User fetchedUser = User.FIND.byId(u.getId());
        assertThat(fetchedUser).isEqualTo(u);
    }

    private Result updateUser(Long id, JsonNode data, User requester) {
        FakeRequest update = fakeRequest().withJsonBody(data);
        if(requester != null) {
            update = authorizeRequest(update, requester);
        }
        Result result = callAction(routes.ref.UserController.update(id), update);
        return result;
    }

    @Test
    public void getAll_UnauthorizedRequest_UnauthorizedReturned() {

        Result result = callAction(routes.ref.UserController.getAll(), fakeRequest());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(), getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void getAll_AuthorizedRequest_SuccessfullyGetAllUsers() {
        Result result = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        JsonNode response = Json.parse(contentAsString(result));
        ArrayNode list = (ArrayNode) removeRootElement(response, User.class);
        List<User> usersFromDB = User.FIND.all();

        // Assure equality
        assertThat(usersFromDB.size()).isEqualTo(list.size());

        Map<Long, String> userMap = new HashMap<>(usersFromDB.size());
        for(User user : usersFromDB) {
            userMap.put(user.getId(), user.getEmail());
        }

        for(JsonNode userNode : list) {
            Long key = userNode.findValue("id").asLong();
            String email = userNode.findValue("email").asText();
            assertThat(userMap.get(key)).isEqualTo(email);
        }

        result = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(OK);
    }
}
