package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.ModelHelper;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by Eveline on 6/03/2015.
 */

@Entity
@JsonRootName("checkpoint")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Checkpoint extends Model {

    public final static Finder<Long, Checkpoint> FIND = new Finder<Long, Checkpoint>(Long.class, Checkpoint.class);

    @Id
    private Long id;

    @Constraints.Required
    private double longitude;

    @Constraints.Required
    private double lattitude;

    @Constraints.Required
    private double altitude;

    @Constraints.Required
    private int waitingTime;

    public Checkpoint (double longitude, double lattitude, double altitude){
        this.longitude = longitude;
        this.lattitude = lattitude;
        this.altitude = altitude;
        this.waitingTime = 0;
    }

    public Checkpoint (double longitude, double lattitude, double altitude, int waitingTime){
        this.longitude = longitude;
        this.lattitude = lattitude;
        this.altitude = altitude;
        this.waitingTime = waitingTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getLattitude() {
        return lattitude;
    }

    public void setLattitude(double lattitude) {
        this.lattitude = lattitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Checkpoint() {
        this(0,0,0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof Checkpoint))
            return false;
        Checkpoint checkpoint = (Checkpoint) obj;
        boolean isEqual = this.id.equals(checkpoint.id);
        isEqual &= ModelHelper.compareFloatingPoints(this.longitude, checkpoint.longitude);
        isEqual &= ModelHelper.compareFloatingPoints(this.lattitude, checkpoint.lattitude);
        isEqual &= ModelHelper.compareFloatingPoints(this.altitude, checkpoint.altitude);
        return isEqual && this.waitingTime == checkpoint.waitingTime;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lattitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(altitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + waitingTime;
        return result;
    }
}
