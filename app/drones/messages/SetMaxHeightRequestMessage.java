package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SetMaxHeightRequestMessage implements Serializable {
    private float meters;

    public SetMaxHeightRequestMessage(float meters) {
        this.meters = meters;
    }

    public float getMeters() {
        return meters;
    }
}
