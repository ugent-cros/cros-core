package drones.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class SetHomeCommand implements Serializable {
    private double latitude, longitude, altitude;

    public SetHomeCommand(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }
}
