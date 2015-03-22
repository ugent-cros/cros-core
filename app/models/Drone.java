package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.JsonHelper;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * Created by matthias on 19/02/2015.
 */

@Entity
@JsonRootName("drone")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Drone extends Model {

    public static final Finder<Long,Drone> FIND = new Finder<>(Long.class, Drone.class);

    @JsonView(JsonHelper.Summary.class)
    @Id
    private Long id;

    @Version
    @JsonIgnore
    public Long version;
    
	@JsonView(JsonHelper.Summary.class)
    @Constraints.Required
    private String name;

    private int weightLimitation;

    @Constraints.Required
    private Status status;

    @Constraints.Required
    private String address;

    @Constraints.Required
    @Embedded
    private DroneType droneType;

    // setting default values
    public Drone() {
        status = Status.AVAILABLE;
    }

    public Drone(String name, Status status, DroneType droneType, String address) {
        this.name = name;
        this.status = status;
        this.address = address;
        this.droneType = droneType;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long stamp) {
        this.version = stamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeightLimitation() {
        return weightLimitation;
    }

    public void setWeightLimitation(int weightLimitation) {
        this.weightLimitation = weightLimitation;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public DroneType getDroneType() {
        return droneType;
    }

    public void setDroneType(DroneType type) {
        droneType = type;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null || !(obj instanceof Drone))
            return false;
        Drone other = (Drone) obj;
        boolean isEqual = this.id.equals(other.id);
        isEqual &= this.name.equals(other.name);
        isEqual &= this.weightLimitation == other.weightLimitation;
        isEqual &= this.status == other.status;
        isEqual &= this.address.equals(other.address);
        return isEqual && this.droneType.equals(other.droneType);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + weightLimitation;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (droneType != null ? droneType.hashCode() : 0);
        return result;
    }

    public enum Status {
        AVAILABLE,
        UNAVAILABLE,
        IN_FLIGHT,
        CHARGING,
        EMERGENCY_LANDED,
        UNKNOWN
    }
}
