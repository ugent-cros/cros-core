package droneapi.messages;

import droneapi.model.properties.FlipType;

import java.io.Serializable;

/**
 * Created by Cedric on 4/21/2015.
 */
public class FlipRequestMessage implements Serializable {
    private FlipType flip;

    public FlipRequestMessage(FlipType flip) {
        this.flip = flip;
    }

    public FlipType getFlip() {
        return flip;
    }
}
