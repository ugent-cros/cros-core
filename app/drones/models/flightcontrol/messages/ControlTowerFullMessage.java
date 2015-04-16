package drones.models.flightcontrol.messages;

import java.io.Serializable;

/**
 * Created by Sander on 10/04/2015.
 */
public class ControlTowerFullMessage implements Serializable {

    private AddDroneMessage m;

    public ControlTowerFullMessage(AddDroneMessage m) {
        this.m = m;
    }

    public AddDroneMessage getM() {
        return m;
    }
}
