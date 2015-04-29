package drones.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SetMaxTiltCommand implements Serializable {
    private float degrees;

    public SetMaxTiltCommand(float degrees) {
        this.degrees = degrees;
    }

    public float getDegrees() {
        return degrees;
    }
}
