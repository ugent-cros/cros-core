package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utilities.annotations.Authenticator;

/**
 * Created by matthias on 20/02/2015.
 */
public class SecurityController extends Controller {

    public static final String AUTH_TOKEN_HEADER = "X-AUTH-TOKEN";
    public static final String AUTH_TOKEN = "authToken";

    public static User getUser() {
        User user = (User) Http.Context.current().args.get("user");
        if(user == null) {
            user = Authenticator.checkAuthentication(Http.Context.current());
        }
        return user;
    }

    // returns an authToken
    public static Result login() {

        // Check form data
        Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
        if (loginForm.hasErrors())
            return badRequest(loginForm.errorsAsJson());

        // Authenticate the user
        Login login = loginForm.get();
        User user = User.authenticate(login.emailAddress, login.password);
        if (user == null)
            return unauthorized();

        // Return auth token to the client
        String authToken = user.getAuthToken();
        ObjectNode authTokenJson = Json.newObject();
        authTokenJson.put(AUTH_TOKEN, authToken);

        return ok(authTokenJson);
    }

    public static class Login {
        @Constraints.Required
        @Constraints.Email
        public String emailAddress;

        @Constraints.Required
        public String password;
    }

}
