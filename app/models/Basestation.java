package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.JsonHelper;

import javax.persistence.*;

/**
 * Created by Eveline on 6/03/2015.
 */
@Entity
@Table(name="basestation")
@JsonRootName("basestation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Basestation extends Model {

    public static final Finder<Long, Basestation> FIND = new Finder<>(Long.class, Basestation.class);

    @Id
    @JsonView(JsonHelper.Summary.class)
    protected Long id;

    @Version
    @JsonIgnore
    private Long version;

    @JsonView(JsonHelper.Summary.class)
    @Constraints.Required
    @Column(length = 256, unique = true, nullable = false)
    private String name;

    @Constraints.Required
    @Embedded
    private Location location;

    public Basestation(String name, double longitude, double latitude, double altitude){
        this.setLocation(new Location(longitude, latitude, altitude));
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Basestation))
            return false;
        Basestation basestation = (Basestation) obj;
        boolean isEqual = (this.name == null && basestation.name == null)
                || (this.name != null && this.name.equals(basestation.name));
        isEqual &= (this.location == null && basestation.location == null)
                || this.location != null && this.location.equals(basestation.location);

        return this.name.equals(basestation.name);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
