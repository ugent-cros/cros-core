package parrot.shared.commands;

import model.properties.FlipType;

import java.io.Serializable;

/**
 * Created by Cedric on 4/21/2015.
 */
public class FlipCommand implements Serializable {
    private FlipType flip;

    public FlipCommand(FlipType flip) {
        this.flip = flip;
    }

    public FlipType getFlip() {
        return flip;
    }
}
