package droneapi.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class SpeedChangedMessage implements Serializable {
    private double speedX;
    private double speedY;
    private double speedZ;

    public SpeedChangedMessage(double speedX, double speedY, double speedZ) {
        this.speedX = speedX;
        this.speedY = speedY;
        this.speedZ = speedZ;
    }

    public double getSpeedX() {
        return speedX;
    }

    public double getSpeedY() {
        return speedY;
    }

    public double getSpeedZ() {
        return speedZ;
    }
}
