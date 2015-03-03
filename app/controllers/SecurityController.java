package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Created by matthias on 20/02/2015.
 */
public class SecurityController extends Controller {

    public static final String AUTH_TOKEN_HEADER = "X-AUTH-TOKEN";
    public static final String AUTH_ID_HEADER = "X-AUTH-ID";
    public static final String AUTH_TOKEN = "authToken";
    public static final String AUTH_ID = "authId";

    public static User getUser() {
        return (User) Http.Context.current().args.get("user");
    }

    // returns an authToken
    public static Result login() {
        Form<Login> loginForm = Form.form(Login.class).bindFromRequest();

        if (loginForm.hasErrors())
            return badRequest(loginForm.errorsAsJson());

        Login login = loginForm.get();
        User user = User.findByEmailAddressAndPassword(login.emailAddress, login.password);

        if (user == null)
            return unauthorized();

        String authToken = user.createToken();
        ObjectNode authTokenJson = Json.newObject();
        authTokenJson.put(AUTH_TOKEN, authToken);
        authTokenJson.put(AUTH_ID, user.id);
        response().setCookie(AUTH_TOKEN, authToken);
        response().setCookie(AUTH_ID,user.id.toString());
        return ok(authTokenJson);
    }

    @Security.Authenticated(Secured.class)
    public static Result logout() {
        response().discardCookie(AUTH_TOKEN);
        getUser().deleteAuthToken();
        return ok();
    }

    public static class Login {
        @Constraints.Required
        @Constraints.Email
        public String emailAddress;

        @Constraints.Required
        public String password;

    }

}
