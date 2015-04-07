package utilities.annotations;

import controllers.SecurityController;
import models.User;
import play.libs.F;
import play.libs.Scala;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import scala.Tuple2;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yasser on 6/03/15.
 */

public class Authenticator extends Action<Authentication> {

    public F.Promise<Result> call(Http.Context context) throws Throwable {

        User user = checkAuthentication(context);

        // Check if a user is logged in
        if (user != null) {
            // Check if user has an allowed role
            User.Role[] allowedRoles = configuration.value();
            for(User.Role role : allowedRoles)  {
                if (role.equals(user.getRole())) {
                    return delegate.call(context);
                }
            }
        }

        List<Tuple2<String, String>> list = new ArrayList<>();
        Tuple2<String, String> h = new Tuple2<>("Access-Control-Allow-Origin","*");
        list.add(h);
        Seq<Tuple2<String, String>> seq = Scala.toSeq(list);
        Result error = () -> Results.unauthorized().toScala().withHeaders(seq);
        return F.Promise.pure(error);
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
