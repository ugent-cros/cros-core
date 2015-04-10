package drones.models.flightcontrol.messages;

/**
 * Created by Sander on 10/04/2015.
 */
public class ControlTowerFullMessage {

    private AddDroneMessage m;

    public ControlTowerFullMessage(AddDroneMessage m) {
        this.m = m;
    }

    public AddDroneMessage getM() {
        return m;
    }
}
