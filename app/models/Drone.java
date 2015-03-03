package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import controllers.routes;
import play.data.validation.Constraints;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthias on 19/02/2015.
 */

@Entity
@JsonRootName("drone")
public class Drone extends Resource {

    @Id
    public Long id;

    @Constraints.Required
    public String name;

    public int weightLimitation;

    @Constraints.Required
    public Status status;

    @Constraints.Required
    public String address;

    @Constraints.Required
    public CommunicationType communicationType;

    @Constraints.Required
    public double longitude;
    @Constraints.Required
    public double latitude;
    @Constraints.Required
    public double altitude;

    // setting default values
    public Drone() {
        status = Status.AVAILABLE;
        longitude = -1.0;
        latitude = -1.0;
        altitude = -1.0;
    }

    public Drone(String name, Status status, CommunicationType communicationType, String address) {
        this.name = name;
        this.status = status;
        this.address = address;
        this.communicationType = communicationType;
    }

    public int getBatteryStatus() {
        return -1;
    }

    @JsonIgnore
    public Location location() {
        return new Location(longitude,latitude,altitude);
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

    public static Finder<Long,Drone> find = new Finder<>(Long.class, Drone.class);

    @Override
    public List<Link> getLinksList() {
        List<Link> links = new ArrayList<>();
        links.add(new Link("self", routes.DroneController.get(id).url()));
        links.add(new Link("all", routes.DroneController.getAll().url()));
        links.add(new Link("add", routes.DroneController.add().url()));
        links.add(new Link("delete", routes.DroneController.delete(id).url()));
        return links;
    }

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
