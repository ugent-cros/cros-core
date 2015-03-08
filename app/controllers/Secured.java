package controllers;

import models.User;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Created by matthias on 20/02/2015.
 */
public class Secured extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {

        String[] authTokenHeaderValues = context.request().headers().get(SecurityController.AUTH_TOKEN_HEADER);

        if (authTokenHeaderValues == null)
            return null;

        boolean headerIsValid = authTokenHeaderValues.length == 1 && authTokenHeaderValues[0] != null;
        if (headerIsValid) {
            User user = models.User.findByAuthToken(authTokenHeaderValues[0]);
            if (user != null) {
                context.args.put("user", user);
                return user.getEmail();
            }
        }

        return null;
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return unauthorized();
    }
}
