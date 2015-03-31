package controllers;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Basestation;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.QueryHelper;
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

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getAll() {
        ExpressionList<User> exp = QueryHelper.buildQuery(User.class, User.FIND.where());

        List<JsonHelper.Tuple> tuples = new ArrayList<>();
        for(User user : exp.findList()) {
            tuples.add(new JsonHelper.Tuple(user, new ControllerHelper.Link("self",
                    controllers.routes.UserController.get(user.getId()).url())));
        }

        // TODO: add links when available
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.UserController.getAll().url()));
        links.add(new ControllerHelper.Link("total", controllers.routes.UserController.getTotal().url()));


        try {
            return ok(JsonHelper.createJsonNode(tuples, links, User.class));
        } catch(JsonProcessingException ex) {
            play.Logger.error(ex.getMessage(), ex);
            return internalServerError();
        }
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getTotal() {
        return ok(JsonHelper.addRootElement(Json.newObject().put("total", User.FIND.findRowCount()), User.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result get(Long id) {
        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(User.Role.USER.equals(client.getRole()) && client.getId() != id)
            return unauthorized();

        User user = User.FIND.byId(id);
        if(user == null)
            return notFound();

        return ok(JsonHelper.createJsonNode(user, getAllLinks(id), User.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result create() {
        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, User.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            play.Logger.debug(ex.getMessage(), ex);
            return badRequest(ex.getMessage());
        }
        Form<User> filledForm = form.bind(strippedBody);

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
        return created(JsonHelper.createJsonNode(newUser, getAllLinks(newUser.getId()), User.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result update(Long id) {
        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!User.Role.ADMIN.equals(client.getRole()) && client.getId() != id)
            return unauthorized();

        // Check if user exists
        User user = User.FIND.byId(id);
        if(user == null)
            return notFound();

        // Check input
        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, User.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            play.Logger.debug(ex.getMessage(), ex);
            return badRequest(ex.getMessage());
        }
        Form<User> filledForm = form.bind(strippedBody);

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

        return ok(JsonHelper.createJsonNode(updatedUser, getAllLinks(id), User.class));
    }

    // no check needed
    public static Result currentUser() {

        User client = SecurityController.getUser();
        if(client == null) {
            return unauthorized();
        }
        return redirect(controllers.routes.UserController.get(client.getId()));
    }

    @Authentication({User.Role.ADMIN})
    public static Result deleteAll() {
        User client = SecurityController.getUser();
        User.FIND.where().ne("id", client.getId()).findList().forEach(u -> u.delete());

        return getAll();
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(Long id) {
        // Check if user exists
        User userToDelete = User.FIND.byId(id);
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
    public static Result getUserAuthToken(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(client.getId() != id) {
            return unauthorized();
        }

        // Return auth token to the client
        String authToken = client.getAuthToken();
        ObjectNode authTokenJson = Json.newObject();
        authTokenJson.put(SecurityController.AUTH_TOKEN, authToken);

        return ok(authTokenJson);
    }

    @Authentication({User.Role.ADMIN, User.Role.USER})
    public static Result invalidateAuthToken(Long userId) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!User.Role.ADMIN.equals(client.getRole())
                && client.getId() != userId) {
            return unauthorized();
        }

        User user = User.FIND.byId(userId);
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
        user.save();

        return ok();
    }

    private static List<ControllerHelper.Link> getAllLinks(long id) {
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", routes.UserController.get(id).url()));
        links.add(new ControllerHelper.Link("getAuthToken", routes.UserController.getUserAuthToken(id).url()));
        links.add(new ControllerHelper.Link("invalidateAuthToken", routes.UserController.invalidateAuthToken(id).url()));
        return links;
    }

}
