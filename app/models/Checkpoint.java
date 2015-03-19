package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import play.data.validation.Constraints;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Eveline on 6/03/2015.
 */

@Entity
@Table(name="checkpoint")
@JsonRootName("checkpoint")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Checkpoint extends Location {

    public final static Finder<Long, Checkpoint> FIND = new Finder<Long, Checkpoint>(Long.class, Checkpoint.class);

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
        boolean isEqual = super.equals(checkpoint);
        return isEqual && this.waitingTime == checkpoint.waitingTime;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + waitingTime;
        return result;
    }
}
