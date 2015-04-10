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
     * @param roll rotate left/right between [-1;1]
     * @param pitch rotate forward/backward between [-1;1]
     * @param yaw rotation wrt start position (along z axis) between [-1;1]
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
