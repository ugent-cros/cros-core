import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.SecurityController;
import controllers.routes;
import exceptions.IncompatibleSystemException;
import models.User;
import org.fest.assertions.Fail;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
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
    // Exceptions should not be thrown
    public void checkPassword_PasswordIsCorrect_TrueReturned() throws IncompatibleSystemException {
        String password = "lolcats1";
        User u = new User("correct.password@user.tests.cros.com", password, "test", "student");
        assertThat(u.checkPassword(password)).isTrue();
    }

    @Test
    // Exceptions should not be thrown
    public void checkPassword_PasswordIsWrong_FalseReturned() throws IncompatibleSystemException {
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
        JsonNode node = JsonHelper.createJsonNode(objectNode, User.class);

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
        JsonNode node = JsonHelper.createJsonNode(objectNode, User.class);

        FakeRequest create = fakeRequest().withJsonBody(node);

        Result result = callAction(routes.ref.UserController.create(), authorizeRequest(create, getAdmin()));
        assertThat(status(result)).isEqualTo(CREATED);

        try {
            User receivedUser =
                    Json.fromJson(removeRootElement(contentAsString(result), User.class, false), User.class);
            u.setId(receivedUser.getId()); // bypass id because u has no id yet
            assertThat(u).isEqualTo(receivedUser);

            User createdUser = User.findByEmail(email);
            assertThat(u).isEqualTo(createdUser);

            createdUser.delete();
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid Json exception: " + ex.getMessage());
        }
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
    public void updatePassword_AuthorizedRequest_UserUpdated() throws IncompatibleSystemException {

        String email = "admin.userupdatepassword@user.tests.cros.com";
        User u = new User(email, "password", "John", "Doe");
        u.save();

        // Send request to update this user
        JsonNode data = JsonHelper.createJsonNode(u, User.class);
        ObjectNode user = (ObjectNode) data.get("user");
        String newPassword = "newPassword";
        user.put("password", newPassword);

        Result result = updateUser(u.getId(), data, getAdmin());
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        User loginUser = User.authenticate(email, newPassword);
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getId()).isEqualTo(u.getId());
    }

    @Test
    public void update_AuthorizedRequest_UserUpdated() {
        User u = new User("admin.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user
        u.setFirstName("Jane");
        JsonNode data = JsonHelper.createJsonNode(u, User.class);

        Result result = updateUser(u.getId(), data, getAdmin());
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        try {
            User receivedUser =
                    Json.fromJson(removeRootElement(contentAsString(result), User.class, false), User.class);
            assertThat(receivedUser).isEqualTo(u);
            User fetchedUser = User.FIND.byId(u.getId());
            assertThat(fetchedUser).isEqualTo(u);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid Json exception: " + ex.getMessage());
        }
    }

    @Test
    public void update_AuthorizedRequestByUser_UserUpdated() {
        User u = new User("himself.userupdate@user.tests.cros.com", "password", "John", "Doe");
        u.save();

        // Send request to update this user
        u.setFirstName("Jane");
        JsonNode data = JsonHelper.createJsonNode(u, User.class);

        Result result = updateUser(u.getId(), data, u);
        assertThat(status(result)).isEqualTo(OK);

        // Check if update was executed
        try {
            User receivedUser =
                    Json.fromJson(removeRootElement(contentAsString(result), User.class, false), User.class);
            assertThat(receivedUser).isEqualTo(u);
            User fetchedUser = User.FIND.byId(u.getId());
            assertThat(fetchedUser).isEqualTo(u);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid Json exception: " + ex.getMessage());
        }
    }

    private Result updateUser(Long id, JsonNode data, User requester) {
        FakeRequest update = fakeRequest().withJsonBody(data);
        if(requester != null) {
            update = authorizeRequest(update, requester);
        }
        return callAction(routes.ref.UserController.update(id), update);
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
        try {
            ArrayNode list = (ArrayNode) removeRootElement(response, User.class, true);
            List<User> usersFromDB = User.FIND.all();

            // Assure equality
            assertThat(usersFromDB.size()).isEqualTo(list.size());

            Map<Long, String> userMap = new HashMap<>(usersFromDB.size());
            for (User user : usersFromDB) {
                userMap.put(user.getId(), user.getEmail());
            }

            for (JsonNode userNode : list) {
                Long key = userNode.findValue("id").asLong();
                String email = userNode.findValue("email").asText();
                assertThat(userMap.get(key)).isEqualTo(email);
            }

            result = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(), getReadOnlyAdmin()));
            assertThat(status(result)).isEqualTo(OK);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid Json exception: " + ex.getMessage());
        }
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
    public void currentUser_AuthorizedRequest_Ok() {

        User user = new User("authorized.currentuser@user.tests.cros.com", "password", "John", "Doe");
        user.save();

        Result result = callAction(routes.ref.UserController.currentUser(),
                authorizeRequest(fakeRequest(), user));
        assertThat(status(result)).isEqualTo(Http.Status.OK);

        try {
            JsonNode responseJson = JsonHelper.removeRootElement(Json.parse(contentAsString(result)), User.class, false);
            assertThat(Json.fromJson(responseJson, User.class)).isEqualTo(user);
        } catch (JsonHelper.InvalidJSONException e) {
            e.printStackTrace();
            Fail.fail(e.getMessage());
        }
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

    @Test
    public void total_UsersInDatabase_TotalIsCorrect() {
        int correctTotal = User.FIND.all().size();
        Result r = callAction(routes.ref.UserController.getTotal(), authorizeRequest(fakeRequest(), getAdmin()));
        try {
            JsonNode responseNode = JsonHelper.removeRootElement(contentAsString(r), User.class, false);
            assertThat(correctTotal).isEqualTo(responseNode.get("total").asInt());
        } catch (JsonHelper.InvalidJSONException e) {
            e.printStackTrace();
            Assert.fail("Invalid json exception" + e.getMessage());
        }
    }
}
