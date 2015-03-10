package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.ControllerHelper;

import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Entity
@Table(name="useraccount")
@JsonRootName("user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends Model {

    public static enum Role {
        USER,
        ADMIN,
        READONLY_ADMIN
    }

    private static final long MIN_PASSWORD_LENGTH = 8;
    public static String validatePassword(String password) {
        if(password == null) {
            return "required";
        }

        if(password.length() < 8) {
            return "min length " + MIN_PASSWORD_LENGTH;
        }

        return null;
    }

    @JsonView({ControllerHelper.Summary.class})
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

    @Column(nullable = false, updatable = false)
    private Date creationDate;

    public Date getCreationDate() {
        return creationDate;
    }

    @JsonView({ControllerHelper.Summary.class})
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

    @JsonIgnore
    @Column(length = 64, nullable = false)
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

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(!(obj instanceof User))
            return false;
        User other = (User) obj;
        return this.id.equals(other.id)
                && this.firstName.equals(other.firstName)
                && this.lastName.equals(other.lastName)
                && this.email.equals(other.email)
                && this.role.equals(other.role);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (shaPassword != null ? Arrays.hashCode(shaPassword) : 0);
        result = 31 * result + (authToken != null ? authToken.hashCode() : 0);
        return result;
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
