package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SetMaxTiltRequestMessage implements Serializable {
    private float degrees;

    public SetMaxTiltRequestMessage(float degrees) {
        this.degrees = degrees;
    }

    public float getDegrees() {
        return degrees;
    }
}
