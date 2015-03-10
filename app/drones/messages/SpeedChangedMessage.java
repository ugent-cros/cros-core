package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class SpeedChangedMessage implements Serializable {
    private float speedX;
    private float speedY;
    private float speedZ;

    public SpeedChangedMessage(float speedX, float speedY, float speedZ) {
        this.speedX = speedX;
        this.speedY = speedY;
        this.speedZ = speedZ;
    }

    public float getSpeedX() {
        return speedX;
    }

    public float getSpeedY() {
        return speedY;
    }

    public float getSpeedZ() {
        return speedZ;
    }
}
