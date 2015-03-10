package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Drone;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Controller.request;
import static play.mvc.Results.*;

/**
 * Created by yasser on 4/03/15.
 */

public class UserController {

    private static ObjectMapper jsonMapper = new ObjectMapper();

    public static final ControllerHelper.Link allUsersLink = new ControllerHelper.Link("users", controllers.routes.UserController.getAll().url());

    private static Form<User> form = Form.form(User.class);
    private static final String PASSWORD_FIELD_KEY = "password";

    public static Result getAll() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

        ArrayNode array = objectMapper.createArrayNode();
        for(User user : User.find.all()) {
            try {
                ObjectNode userNode = (ObjectNode) Json.parse(objectMapper.writerWithView(ControllerHelper.Summary.class).writeValueAsString(user));

                List<ControllerHelper.Link> links = new ArrayList<>();
                links.add(new ControllerHelper.Link("details", controllers.routes.UserController.get(user.id).url()));
                userNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(userNode);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return internalServerError();
            }
        }

        ObjectNode node = (ObjectNode) JsonHelper.addRootElement(array, Drone.class);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.UserController.getAll().url()));
        links.add(new ControllerHelper.Link("create", controllers.routes.UserController.create().url()));
        node.put("links", (JsonNode) objectMapper.valueToTree(links));

        return ok(node);
    }

    @Authentication({User.Role.ADMIN})
    public static Result create() {
        JsonNode postData = JsonHelper.removeRootElement(request().body().asJson(), User.class);
        Form<User> filledForm = form.bind(postData);

        // Check password
        Form.Field passwordField = filledForm.field(PASSWORD_FIELD_KEY);
        String passwordError = User.validatePassword(passwordField.value());
        if(passwordError != null) {
            filledForm.reject(PASSWORD_FIELD_KEY, passwordError);
        }

        if(filledForm.hasErrors()) {
            // maybe create info about what's wrong
            return badRequest(filledForm.errorsAsJson());
        }

        User newUser = filledForm.get();

        // Check if a user with this email address already exists
        if(User.findByEmail(newUser.getEmail()) != null) {
            return badRequest("Email address is already in use.");
        }

        // Create new user
        newUser.save();

        // Add user to result
        ObjectNode node = (ObjectNode) Json.toJson(newUser);

        // Add links to result
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(Application.homeLink);
        links.add(allUsersLink);
        node.put("links", (JsonNode) jsonMapper.valueToTree(links));

        return created(JsonHelper.addRootElement(node,User.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result deleteAll() {
        User client = SecurityController.getUser();
        User.find.where().ne("id", client.id).findList().forEach(u -> u.delete());

        return getAll();
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(Long id) {
        // Check if user exists
        User userToDelete = User.find.byId(id);
        if(userToDelete == null) {
            return notFound();
        }

        // Delete the user
        userToDelete.delete();

        // Add links to result
        ObjectNode root = jsonMapper.createObjectNode();
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(Application.homeLink);
        links.add(allUsersLink);
        root.put("links", (JsonNode) jsonMapper.valueToTree(links));

        return ok(root);
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result get(Long id) {
        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(User.Role.USER.equals(client.role) && client.id != id)
            return unauthorized();

        User user = User.find.byId(id);
        if(user == null)
            return notFound();

        // Add user to result
        ObjectNode root = (ObjectNode) Json.toJson(user);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(Application.homeLink);
        links.add(allUsersLink);
        root.put("links", (JsonNode) jsonMapper.valueToTree(links));

        return ok(JsonHelper.addRootElement(root,User.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result getUserAuthToken(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(client.id != id) {
            return unauthorized();
        }

        return ok(Json.toJson(client.getAuthToken()));
    }

    @Authentication({User.Role.ADMIN, User.Role.USER})
    public static Result invalidateAuthToken(Long userId) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!User.Role.ADMIN.equals(client.role)
                && client.id != userId) {
            return unauthorized();
        }

        User user = User.find.byId(userId);
        if(user == null) {

            // Return possible links
            ObjectNode root = jsonMapper.createObjectNode();
            List<ControllerHelper.Link> links = new ArrayList<>();
            links.add(Application.homeLink);
            links.add(allUsersLink);
            root.put("links", (JsonNode) jsonMapper.valueToTree(links));

            return notFound();
        }

        user.invalidateAuthToken();

        return ok();
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result update(Long id) {
        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!User.Role.ADMIN.equals(client.role) && client.id != id)
            return unauthorized();

        // Check if user exists
        User user = User.find.byId(id);
        if(user == null)
            return notFound();

        // Check input
        JsonNode postData = JsonHelper.removeRootElement(request().body().asJson(), User.class);
        Form<User> filledForm = form.bind(postData);

        // Check if password is long enough in filled form
        String password = filledForm.field(PASSWORD_FIELD_KEY).value();
        if(password != null) {
            String error = User.validatePassword(password);
            if(error != null) {
                filledForm.reject(PASSWORD_FIELD_KEY, error);
            }
        }
        // Check rest of input
        if(filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        }

        // Update the user
        User updatedUser = filledForm.get();
        updatedUser.update(id);

        return get(updatedUser.id);
    }

    // no check needed
    public static Result currentUser() {

        User client = SecurityController.getUser();
        if(client == null) {
            return notFound();
        }
        return redirect(controllers.routes.UserController.get(client.id));
    }
}
