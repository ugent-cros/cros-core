package drones.models.flightcontrol;

import drones.models.DroneCommander;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control for one single drone = pilot of the drone.
 */
public abstract class Pilot extends FlightControl{

    protected DroneCommander dc;

    public Pilot(DroneCommander dc) {
        super();
        this.dc = dc;
    }
}
