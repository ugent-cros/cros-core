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
        User user;
        String[] authTokenHeaderValues = context.request().headers().get(SecurityController.AUTH_TOKEN_HEADER);
        String[] authIdHeaderValues = context.request().headers().get(SecurityController.AUTH_ID_HEADER);

        if (authTokenHeaderValues == null)
            return null;
        if (authIdHeaderValues == null)
            return null;

        if (headersValid(authTokenHeaderValues, authIdHeaderValues)) {
            user = models.User.findByAuthToken(authTokenHeaderValues[0]);
            if (user != null && user.id == Integer.parseInt(authIdHeaderValues[0])) {
                context.args.put("user", user);
                return user.getEmailAddress();
            }
        }

        return null;
    }

    private boolean headersValid(String[] authTokenHeaderValues, String[] authIdHeaderValues) {
        return authTokenHeaderValues.length == 1 && authTokenHeaderValues[0] != null && authIdHeaderValues.length == 1 && authIdHeaderValues[0] != null;
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return unauthorized();
    }
}
