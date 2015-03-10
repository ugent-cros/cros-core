package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.ControllerHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.beans.Transient;

/**
 * Created by matthias on 19/02/2015.
 */

@Entity
@JsonRootName("drone")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Drone extends Model {

    @JsonView(ControllerHelper.Summary.class)
    @Id
    public Long id;

    @JsonView(ControllerHelper.Summary.class)
    @Constraints.Required
    public String name;

    public int weightLimitation;

    @Constraints.Required
    public Status status;

    @Constraints.Required
    public String address;

    @Constraints.Required
    public CommunicationType communicationType;

    // setting default values
    public Drone() {
        status = Status.AVAILABLE;
    }

    public Drone(String name, Status status, CommunicationType communicationType, String address) {
        this.name = name;
        this.status = status;
        this.address = address;
        this.communicationType = communicationType;
    }

    @Transient
    public int getBatteryStatus() {
        return -1;
    }

    @Transient
    public Location getLocation() {
        return new Location();
    }

    public String getCameraCapture() {
        return "string to image not yet implemented";
    }

    public boolean testConnection() {
        return true; // TODO: implement
    }

    public void emergency() {
        // TODO: implement
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(!(obj instanceof Drone))
            return false;
        Drone other = (Drone) obj;
        return this.id.equals(other.id)
                && this.name.equals(other.name)
                && this.weightLimitation == other.weightLimitation
                && this.status == other.status
                && this.address.equals(other.address)
                && this.communicationType == other.communicationType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + weightLimitation;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (communicationType != null ? communicationType.hashCode() : 0);
        return result;
    }

    public static Finder<Long,Drone> find = new Finder<>(Long.class, Drone.class);

    @JsonRootName("location")
    public class Location {
        @Constraints.Required
        public double longitude;
        @Constraints.Required
        public double latitude;
        @Constraints.Required
        public double altitude;

        public Location() {
            longitude = 0;
            latitude = 0;
            altitude = 0;
        }

        public Location(double longitude, double latitude, double altitude) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.altitude = altitude;
        }
    }

    public enum Status {
        AVAILABLE,
        UNAVAILABLE,
        IN_FLIGHT,
        CHARGING,
        EMERGENCY_LANDED,
        UNKNOWN
    }

    public enum CommunicationType {
        DEFAULT
    }
}
