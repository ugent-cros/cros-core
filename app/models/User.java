package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import exceptions.IncompatibleSystemException;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.ControllerHelper;

import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name="useraccount")
@JsonRootName("user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends Model {

    private static final String PASSWORD_HASH_METHOD = "SHA-512";
    public static final String PASSWORD_ENCODING = "UTF-8";

    private static final long MIN_PASSWORD_LENGTH = 8;
    public static final Finder<Long, User> FIND = new Finder<Long, User>(Long.class, User.class);

    @JsonView({ControllerHelper.Summary.class})
    @Id
    private Long id;

    @Column(length = 256, nullable = false)
    @Constraints.Required
    @Constraints.MinLength(1)
    @Constraints.MaxLength(256)
    private String firstName;

    @Column(length = 256, nullable = false)
    @Constraints.Required
    @Constraints.MinLength(1)
    @Constraints.MaxLength(256)
    private String lastName;

    @Column(nullable = false, updatable = false)
    private Date creationDate;

    @JsonView({ControllerHelper.Summary.class})
    @Column(length = 256, unique = true, nullable = false)
    @Constraints.MaxLength(256)
    @Constraints.Required
    @Constraints.Email
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @JsonIgnore
    @Column(length = 64, nullable = false)
    private byte[] shaPassword;

    @JsonIgnore
    private boolean passwordHashed;

    // TODO: allow multiple browsers
    @JsonIgnore
    private String authToken;

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

    public Date getCreationDate() {
        return creationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public byte[] getShaPassword() {
        return shaPassword;
    }

    public void setPassword(String password) {
        try {
            // Hash password and save it
            shaPassword = getSha512(password);
            passwordHashed = true;
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // If hashing fails, save the password in plain text
            play.Logger.info("Hashing of password failed, saving in plain text instead", e);
            shaPassword = password.getBytes();
            passwordHashed = false;
        }
    }

    public String getAuthToken() {

        // create a token if non exists for this user
        if(authToken == null) {
            authToken = UUID.randomUUID().toString();
            save();
        }

        return authToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email.toLowerCase();
    }

    public static byte[] getSha512(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest hashMethod = MessageDigest.getInstance(PASSWORD_HASH_METHOD);
        byte[] input = value.getBytes(PASSWORD_ENCODING);
        return hashMethod.digest(input);
    }

    public boolean checkPassword(String password) throws IncompatibleSystemException {

        byte[] input = null;
        if(passwordHashed) {
            try {
                input = getSha512(password);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                play.Logger.error("Password was originally hashed, but hashing method failed when checking password.", e);
                throw new IncompatibleSystemException("Password was hashed with a method unavailable on this system", e);
            }
        } else {
            input = password.getBytes();
        }

        return Arrays.equals(shaPassword, input);
    }

    public static User findByAuthToken(String authToken) {
        if (authToken == null)
            return null;

        try  {
            return FIND.where().eq("authToken", authToken).findUnique();
        } catch (Exception e) {
            play.Logger.error(e.getMessage(), e);
            return null;
        }
    }

    public void invalidateAuthToken() {
        authToken = null;
        save();
    }


    public static User findByEmail(String emailAddress) {
        return FIND.where().eq("email", emailAddress.toLowerCase()).findUnique();
    }

    public static User authenticate(String email, String password) throws IncompatibleSystemException {

        User user = findByEmail(email);
        if(user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    public static String validatePassword(String password) {
        if(password == null) {
            return "required";
        }

        if(password.length() < 8) {
            return "min length " + MIN_PASSWORD_LENGTH;
        }

        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null || !(obj instanceof User))
            return false;
        User other = (User) obj;
        boolean isEqual = this.id.equals(other.id);
        isEqual &= this.firstName.equals(other.firstName);
        isEqual &= this.lastName.equals(other.lastName);
        isEqual &= this.email.equals(other.email);
        return isEqual && this.role.equals(other.role);
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

    public static enum Role {
        USER,
        ADMIN,
        READONLY_ADMIN
    }
}
