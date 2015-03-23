package drones.models.flightcontrol;

import java.io.Serializable;

/**
 * Created by Sander on 18/03/2015.
 *
 * Message to set the altitude.
 */
public class SetAltitudeMessage implements Serializable {

    private double altitude;

    public SetAltitudeMessage(double altitude) {
        this.altitude = altitude;
    }

    public double getAltitude() {
        return altitude;
    }
}
