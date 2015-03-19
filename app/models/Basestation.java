package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import utilities.JsonHelper;

import javax.persistence.*;

/**
 * Created by Eveline on 6/03/2015.
 */
@Entity
@Table(name="basestation")
@JsonRootName("basestation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Basestation extends Location {

    public final static Finder<Long, Basestation> FIND = new Finder<>(Long.class, Basestation.class);

    @JsonView(JsonHelper.Summary.class)
    @Constraints.Required
    @Column(length = 256, unique = true, nullable = false)
    private String name;

    public Basestation(String name, double longitude, double latitude, double altitude){
        super(longitude,latitude, altitude);
        this.name = name;
    }

    public Basestation() { }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return this.name.equals(basestation.name)
                && super.equals(basestation);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
