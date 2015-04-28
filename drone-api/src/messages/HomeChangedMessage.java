package messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class HomeChangedMessage implements Serializable {
    double latitude, longitude, altitude;

    public HomeChangedMessage(double latitude, double longitude, double altitude) {
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
