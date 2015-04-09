package drones.models.flightcontrol.messages;

import models.Drone;

/**
 * Created by Sander on 10/04/2015.
 */
public class AddDroneMessage {

    private Drone drone;

    public AddDroneMessage(Drone drone) {
        this.drone = drone;
    }

    public Drone getDrone() {
        return drone;
    }
}
