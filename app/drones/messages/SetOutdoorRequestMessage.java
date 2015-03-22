package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/22/2015.
 */
public class SetOutdoorRequestMessage implements Serializable {
    boolean outdoor;

    public SetOutdoorRequestMessage(boolean outdoor) {
        this.outdoor = outdoor;
    }

    public boolean isOutdoor() {
        return outdoor;
    }
}
