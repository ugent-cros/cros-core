package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class AttitudeChangedMessage implements Serializable{
    private double roll;
    private double pitch;
    private double yaw;

    public AttitudeChangedMessage(double roll, double pitch, double yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public double getRoll() {
        return roll;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }
}
