package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Controller.request;
import static play.mvc.Controller.session;
import static play.mvc.Results.*;

/**
 * Created by yasser on 4/03/15.
 */

@Security.Authenticated(Secured.class)
public class UserController {


    private static Form<User> form = Form.form(User.class);

    public static Result allUsers() {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!client.hasRole(User.UserRole.ADMIN)
                && !client.hasRole(User.UserRole.READONLY_ADMIN)) {
            return unauthorized();
        }

        List<User> allUsers = User.find.all();
        return ok(Json.toJson(allUsers));
    }

    public static Result createUser() {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!client.hasRole(User.UserRole.ADMIN)) {
            return unauthorized();
        }

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

    public static Result deleteUser(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!client.hasRole(User.UserRole.ADMIN)) {
            return unauthorized();
        }

        // Check if user exists
        User userToDelete = User.find.byId(id);
        if(userToDelete == null) {
            return notFound("No such user");
        }

        // Delete the user
        userToDelete.delete();
        return ok("User was deleted");
    }

    public static Result getUser(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!client.hasRole(User.UserRole.ADMIN) && client.id != id) {
            return unauthorized();
        }

        User user = User.find.byId(id);
        if(user == null) {
            return notFound("No such user");
        } else {

            JsonNode result = Json.toJson(user);

            // Add auth token if user is requesting his own info
            if(client.id == id) {
                // TODO: add auth token
            }

            return ok(result);
        }
    }

    public static Result updateUser(Long id) {

        // Check if user has correct privileges
        User client = SecurityController.getUser();
        if(!client.hasRole(User.UserRole.ADMIN) && client.id != id) {
            return unauthorized();
        }

        // Check if user exists
        User user = User.find.byId(id);
        if(user == null) {
            return notFound("No such user");
        }

        // Added posted info + existing info about user
        Form<User> userForm = form.fill(user);
        Form<User> filledForm = form.bindFromRequest();
        userForm.bind(filledForm.data());

        // Check if password is long enough in filled form
        String password = filledForm.field("password").value();
        if(password != null && !User.PasswordValidator.isValid(password)) {
            userForm.reject("password", (String)User.PasswordValidator.getErrorMessageKey()._1);
        }
        // Check rest of input
        if(userForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        }

        // Update the user
        User updatedUser = userForm.get();
        user.update(updatedUser);

        return ok();
    }
}
