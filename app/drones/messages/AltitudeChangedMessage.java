package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class AltitudeChangedMessage implements Serializable {
    double altitude;

    public AltitudeChangedMessage(double altitude) {
        this.altitude = altitude;
    }

    public double getAltitude() {
        return altitude;
    }
}
