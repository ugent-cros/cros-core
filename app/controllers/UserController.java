package controllers;

import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.ValidationError;
import play.libs.Json;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.annotations.Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static play.mvc.Controller.request;
import static play.mvc.Controller.session;
import static play.mvc.Results.*;

/**
 * Created by yasser on 4/03/15.
 */

public class UserController {


    private static Form<User> form = Form.form(User.class);

    public static Result allUsers() {

        List<User> allUsers = User.find.all();
        return ok(Json.toJson(allUsers));
    }

    @Authentication({User.Role.ADMIN})
    public static Result createUser() {

        Form<User> filledForm = form.bindFromRequest();

        // Check password
        if(!User.PasswordValidator.isValid(filledForm.field("password").value())) {
            filledForm.reject("password", (String)User.PasswordValidator.getErrorMessageKey()._1);
        }

        if(filledForm.hasErrors()) {
            // maybe add info about what's wrong
            return badRequest(filledForm.errorsAsJson());
        }

        User newUser = filledForm.get();

        // Check if a user with this email address already exists
        if(User.findByEmail(newUser.getEmail()) != null) {
            return badRequest("Email address is already in use.");
        }

        // Create new user
        newUser.save();
        return created(Json.toJson((newUser)));
    }

    @Authentication({User.Role.ADMIN})
    public static Result deleteUser(Long id) {

        // Check if user exists
        User userToDelete = User.find.byId(id);
        if(userToDelete == null) {
            return notFound("No such user");
        }

        // Delete the user
        userToDelete.delete();
        return ok("User was deleted");
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result getUser(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(User.Role.USER.equals(client.role) && client.id != id) {
            return unauthorized();
        }

        User user = User.find.byId(id);
        if(user == null) {
            return notFound("No such user");
        }

        return ok(Json.toJson(user));
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
            return notFound("No such user");
        }

        user.invalidateAuthToken();

        return ok();
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
    public static Result updateUser(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!User.Role.ADMIN.equals(client.role) && client.id != id) {
            return unauthorized();
        }

        // Check if user exists
        User user = User.find.byId(id);
        if(user == null) {
            return notFound("No such user");
        }

        // Check input
        Form<User> filledForm = form.bindFromRequest();

        // Check if password is long enough in filled form
        String password = filledForm.field("password").value();
        if(password != null && !User.PasswordValidator.isValid(password)) {
            filledForm.reject("password", (String)User.PasswordValidator.getErrorMessageKey()._1);
        }
        // Check rest of input
        if(filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        }

        // Update the user
        User updatedUser = filledForm.get();
        updatedUser.update(id);

        return ok();
    }

    // no check needed
    public static Result currentUser() {

        User client = SecurityController.getUser();
        if(client == null) {
            return notFound();
        }
        return redirect(controllers.routes.UserController.getUser(client.id));
    }
}
