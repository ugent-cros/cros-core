package models;

import com.avaje.ebean.annotation.PrivateOwned;
import com.fasterxml.jackson.annotation.JsonIgnore;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Entity
@Table(name="useraccount")
public class User extends Model {

    public static Constraints.Validator PasswordValidator = Constraints.minLength(8);

    public static enum Role {
        USER,
        ADMIN,
        READONLY_ADMIN
    }

    @Id
    public Long id;

    @Column(length = 256, nullable = false)
    @Constraints.Required
    @Constraints.MinLength(1)
    @Constraints.MaxLength(256)
    public String firstName;

    @Column(length = 256, nullable = false)
    @Constraints.Required
    @Constraints.MinLength(1)
    @Constraints.MaxLength(256)
    public String lastName;

    @Column(nullable = false)
    public Date creationDate;

    @Column(length = 256, unique = true, nullable = false)
    @Constraints.MaxLength(256)
    @Constraints.Required
    @Constraints.Email
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email.toLowerCase();
    }

    @Enumerated(EnumType.STRING)
    public Role role;

    @Column(length = 64, nullable = false)
    @JsonIgnore
    private byte[] shaPassword;

    public void setPassword(String password) {
        shaPassword = getSha512(password);
    }

    public boolean checkPassword(String password) {
        byte[] hash = getSha512(password);
        return Arrays.equals(shaPassword, hash);
    }

    // TODO: allow multiple browsers
    @JsonIgnore
    private String authToken;

    public String getAuthToken() {

        // create a token if non exists for this user
        if(authToken == null) {
            authToken = UUID.randomUUID().toString();
            save();
        }

        return authToken;
    }

    public void invalidateAuthToken() {
        authToken = null;
        save();
    }

    public User() {
        this.authToken = UUID.randomUUID().toString();
        role = Role.USER;
        this.creationDate = new Date();
    }

    public User(String email, String password, String firstName, String lastName) {
        this();
        setEmail(email);
        setPassword(password);
        this.firstName = firstName;
        this.lastName = lastName;
    }


    public static byte[] getSha512(String value) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Finder<Long, User> find = new Finder<Long, User>(Long.class, User.class);

    public static User findByAuthToken(String authToken) {
        if (authToken == null)
            return null;

        try  {
            return find.where().eq("authToken", authToken).findUnique();
        } catch (Exception e) {
            return null;
        }
    }

    public static User findByEmail(String emailAddress) {
        return find.where().eq("email", emailAddress.toLowerCase()).findUnique();
    }

    public static User authenticate(String email, String password) {

        User user = findByEmail(email);
        if(user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }
}
