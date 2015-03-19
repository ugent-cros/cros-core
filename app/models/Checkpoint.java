package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import utilities.ControllerHelper;
import utilities.Location;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by Eveline on 6/03/2015.
 */

@Entity
@JsonRootName("checkpoint")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Checkpoint extends Location {

    public final static Finder<Long, Checkpoint> FIND = new Finder<Long, Checkpoint>(Long.class, Checkpoint.class);

    @Id
    @JsonView(ControllerHelper.Summary.class)
    private Long id;

    @Constraints.Required
    private int waitingTime;

    public Checkpoint() {
        this(0,0,0);
    }

    public Checkpoint (double longitude, double lattitude, double altitude){
        this.longitude = longitude;
        this.latitude = lattitude;
        this.altitude = altitude;
        this.waitingTime = 0;
    }

    public Checkpoint (double longitude, double lattitude, double altitude, int waitingTime){
        this.longitude = longitude;
        this.latitude = lattitude;
        this.altitude = altitude;
        this.waitingTime = waitingTime;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof Checkpoint))
            return false;
        Checkpoint checkpoint = (Checkpoint) obj;
        boolean isEqual = this.id.equals(checkpoint.id);
        isEqual &= super.equals(checkpoint);
        return isEqual && this.waitingTime == checkpoint.waitingTime;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + waitingTime;
        return result;
    }
}
