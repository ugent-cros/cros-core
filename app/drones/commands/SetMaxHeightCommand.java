package drones.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SetMaxHeightCommand implements Serializable {
    private float meters;

    public SetMaxHeightCommand(float meters) {
        this.meters = meters;
    }

    public float getMeters() {
        return meters;
    }
}
