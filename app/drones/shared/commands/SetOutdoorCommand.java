package drones.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/9/2015.
 */
public class SetOutdoorCommand implements Serializable {
    private boolean outdoor;

    public SetOutdoorCommand(boolean outdoor) {
        this.outdoor = outdoor;
    }

    public boolean isOutdoor() {
        return outdoor;
    }
}
