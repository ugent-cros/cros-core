package drones.models;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class Rotation implements Serializable{
    private double roll;
    private double pitch;
    private double yaw;

    /***
     * Orientation of drone in a 3D plane
     * @param roll Angle left/right in radians
     * @param pitch Angle forward/backward in radians
     * @param yaw Rotation relative to takeoff orientation in radians
     */

    public Rotation(double roll, double pitch, double yaw) {
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
