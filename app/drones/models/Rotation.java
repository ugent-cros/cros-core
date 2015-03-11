package drones.models;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class Rotation implements Serializable{
    private float roll;
    private float pitch;
    private float yaw;

    public Rotation(float roll, float pitch, float yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public float getRoll() {
        return roll;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }
}
