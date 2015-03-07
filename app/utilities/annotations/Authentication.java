package utilities.annotations;

import models.User;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by yasser on 6/03/15.
 */

@With(Authenticator.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)

/**
 * This is the annotation that can be put in front of every method and every class
 * It will check whether the user is authorised to call this specific method.
 * In case of it being on top of a class, all methods will get the same authorisation level.
 *
 * The autorisation level is assigned with the Roles enum from de database.record package.
 */
public @interface Authentication {
    User.Role[] value();
}