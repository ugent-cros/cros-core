package drones.models.flightcontrol;

import java.io.Serializable;

/**
 * Created by Sander on 18/03/2015.
 *
 * Message to set the cruisingAltitude.
 */
public class SetCruisingAltitudeMessage implements Serializable {

    private double cruisingAltitude;

    public SetCruisingAltitudeMessage(double cruisingAltitude) {
        this.cruisingAltitude = cruisingAltitude;
    }

    public double getCruisingAltitude() {
        return cruisingAltitude;
    }
}
