package models;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.JsonHelper;
import utilities.ModelHelper;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Created by matthias on 19/03/2015.
 */
@JsonRootName("location")
@MappedSuperclass
public class Location extends Model {

    @Id
    @JsonView(JsonHelper.Summary.class)
    protected Long id;

    @Constraints.Required
    protected double longitude;
    @Constraints.Required
    protected double latitude;
    @Constraints.Required
    protected double altitude;

    public Location() {
        this(0.0,0.0,0.0);
    }

    public Location(double longitude, double latitude, double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof Location))
            return false;
        Location checkpoint = (Location) obj;
        boolean isEqual = ModelHelper.compareFloatingPoints(this.longitude, checkpoint.longitude);
        isEqual &= this.id == checkpoint.id;
        isEqual &= ModelHelper.compareFloatingPoints(this.latitude, checkpoint.latitude);
        return isEqual && ModelHelper.compareFloatingPoints(this.altitude, checkpoint.altitude);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(altitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}

