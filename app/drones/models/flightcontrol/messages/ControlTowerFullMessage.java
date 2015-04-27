package drones.models.flightcontrol.messages;

/**
 * Created by Sander on 26/03/2015.
 */
public class ControlTowerFullMessage {

    private AddDroneMessage m;

    public ControlTowerFullMessage(AddDroneMessage m) {
        this.m = m;
    }
}
