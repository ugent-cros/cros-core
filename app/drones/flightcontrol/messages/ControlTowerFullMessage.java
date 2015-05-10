package drones.flightcontrol.messages;

/**
 * Sent from the controlTower when more flights ara added than allowed.
 *
 * Created by Sander on 26/03/2015.
 */
public class ControlTowerFullMessage {

    private AddFlightMessage m;

    public ControlTowerFullMessage(AddFlightMessage m) {
        this.m = m;
    }
}
