package drones.models.scheduler;

import drones.models.DroneCommander;
import drones.models.Location;

/**
 * Created by Ronald on 18/03/2015.
 */
public class DroneArrivalMessage {

    private DroneCommander commander;
    private Location location;

    public DroneArrivalMessage(DroneCommander commander, Location location) {
        this.commander = commander;
        this.location = location;
    }

    public DroneCommander getCommander() {
        return commander;
    }

    public Location getLocation() {
        return location;
    }
}
