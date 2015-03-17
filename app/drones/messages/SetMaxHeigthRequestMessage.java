package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SetMaxHeigthRequestMessage implements Serializable {
    private float meters;

    public SetMaxHeigthRequestMessage(float meters) {
        this.meters = meters;
    }

    public float getMeters() {
        return meters;
    }
}
