package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.JsonHelper;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;

/**
 * Created by Eveline on 6/03/2015.
 */

@Entity
@Table(name="checkpoint")
@JsonRootName("checkpoint")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Checkpoint extends Model {

    public static final Finder<Long, Checkpoint> FIND = new Model.Finder<Long, Checkpoint>(Long.class, Checkpoint.class);

    @Id
    @JsonView(JsonHelper.Summary.class)
    protected Long id;

    @Constraints.Required
    @Constraints.Min(value=0)
    private int waitingTime;

    @Constraints.Required
    @Valid
    @Embedded
    private Location location;

    public Checkpoint() {
        this(0,0,0);
    }

    public Checkpoint (double latitude, double longitude, double altitude){
        this(latitude, longitude, altitude, 0);
    }

    public Checkpoint (double latitude, double longitude, double altitude, int waitingTime){
        this.setLocation(new Location(latitude, longitude, altitude));
        this.waitingTime = waitingTime;
    }

    public Checkpoint(Location location){
        this.setLocation(location);
        this.waitingTime = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof Checkpoint))
            return false;
        Checkpoint checkpoint = (Checkpoint) obj;
        boolean isEqual = this.location == null && checkpoint.location == null;
        isEqual |= this.location != null && this.location.equals(checkpoint.location);
        return isEqual && this.waitingTime == checkpoint.waitingTime;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + waitingTime;
        return result;
    }
}
