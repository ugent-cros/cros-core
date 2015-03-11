package drones.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/9/2015.
 */
public class OutdoorCommand implements Serializable {
    private boolean outdoor;

    public OutdoorCommand(boolean outdoor) {
        this.outdoor = outdoor;
    }

    public boolean isOutdoor() {
        return outdoor;
    }
}
