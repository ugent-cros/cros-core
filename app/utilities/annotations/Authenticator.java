package utilities.annotations;

import controllers.SecurityController;
import models.User;
import play.libs.F;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Created by yasser on 6/03/15.
 */

public class Authenticator extends Action<Authentication> {

    public F.Promise<Result> call(Http.Context context) {

        User user = checkAuthentication(context);

        // Check if a user is logged in
        if (user != null) {
            // Check if user has an allowed role
            User.Role[] allowedRoles = configuration.value();
            for(User.Role role : allowedRoles)  {
                if (role.equals(user.role)) {
                    try {
                        return delegate.call(context);
                    } catch (Throwable throwable) {
                        play.Logger.error(throwable.getMessage(), throwable);
                        return F.Promise.pure(internalServerError());
                    }
                }
            }
        }

        return F.Promise.pure(unauthorized());
    }

    public static User checkAuthentication(Http.Context context) {

        String[] authTokenHeaderValues = context.request().headers().get(SecurityController.AUTH_TOKEN_HEADER);

        if (authTokenHeaderValues == null)
            return null;

        boolean headerIsValid = authTokenHeaderValues.length == 1 && authTokenHeaderValues[0] != null;
        if (headerIsValid) {
            User user = models.User.findByAuthToken(authTokenHeaderValues[0]);
            if (user != null) {
                context.args.put("user", user);
                return user;
            }
        }

        return null;
    }
}
