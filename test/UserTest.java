import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.SecurityController;
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

    @Test
    public void delete_UnauthorizedRequest_UnauthorizedReturned() {

        User user = new User("unauthorized.delete@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.delete(user.getId()), fakeRequest());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.delete(user.getId()), authorizeRequest(fakeRequest(), getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.delete(user.getId()), authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        assertThat(User.FIND.byId(user.getId())).isNotNull();
    }

    @Test
    public void delete_AuthorizedRequestNonExistingUser_NotFoundReturned() {

        Long nonExistingId = (long)-1;
        Result result = callAction(routes.ref.UserController.delete(nonExistingId), authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(NOT_FOUND);
    }

    @Test
    public void delete_AuthorizedRequest_UserDeleted() {

        User user = new User("authorized.delete@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.delete(user.getId()), authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        assertThat(User.FIND.byId(user.getId())).isNull();
    }

    @Test
    public void getAuthToken_UnauthorizedRequest_UnauthorizedReturned() {

        User user = new User("unauthorized.token@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.getUserAuthToken(user.getId()), fakeRequest());
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.getUserAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), getUser()));
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.getUserAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.getUserAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void getAuthToken_AuthorizedRequest_TokenReturned() {

        User user = new User("authorized.token@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.getUserAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), user));
        assertThat(status(result)).isEqualTo(OK);

        JsonNode response = Json.parse(contentAsString(result));
        String token = response.findValue(SecurityController.AUTH_TOKEN).asText();
        assertThat(token).isEqualTo(user.getAuthToken());
    }

    @Test
    public void invalidateAuthToken_UnauthorizedRequest_UnauthorizedReturned() {

        User user = new User("unauthorized.invalidatetoken@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.invalidateAuthToken(user.getId()), fakeRequest());
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.invalidateAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), getUser()));
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.invalidateAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
        assertThat(contentAsString(result)).doesNotContain(user.getAuthToken());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void invalidateAuthToken_AuthorizedRequest_TokenInvalidated() {

        User user = new User("authorized.invalidatetoken@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        String oldToken = user.getAuthToken();

        Result result = callAction(routes.ref.UserController.invalidateAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), user));
        assertThat(status(result)).isEqualTo(OK);

        user.refresh();
        assertThat(oldToken).isNotEqualTo(user.getAuthToken());

        oldToken = user.getAuthToken();

        callAction(routes.ref.UserController.invalidateAuthToken(user.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        // getAuthToken should return a new generated token
        user.refresh();
        assertThat(oldToken).isNotEqualTo(user.getAuthToken());
    }

    @Test
    public void currentUser_UnauthorizedRequest_UnauthorizedReturned() {

        Result result = callAction(routes.ref.UserController.currentUser(),fakeRequest());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void currentUser_AuthorizedRequest_Redirect() {

        User user = new User("authorized.currentuser@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.currentUser(),
                authorizeRequest(fakeRequest(), user));
        assertThat(status(result)).isEqualTo(SEE_OTHER);
        assertThat(redirectLocation(result)).isEqualTo(controllers.routes.UserController.get(user.getId()).url());
    }

    @Test
    public void deleteAll_UnauthorizedRequest_UnauthorizedReturned() {

        Result result = callAction(routes.ref.UserController.deleteAll(),fakeRequest());
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.deleteAll(),
                authorizeRequest(fakeRequest(), getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(routes.ref.UserController.deleteAll(),
                authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void deleteAll_AuthorizedRequest_AllButUserDeleted() {

        Result result = callAction(routes.ref.UserController.deleteAll(),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        List<User> allUsers = User.FIND.all();
        assertThat(allUsers).hasSize(1);
        assertThat(allUsers).contains(getAdmin()); // Equality check
    }
}
