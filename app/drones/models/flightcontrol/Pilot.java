package drones.models.flightcontrol;

import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control for one single drone = pilot of the drone.
 */
public abstract class Pilot extends FlightControl{

    protected Drone drone;
    protected DroneCommander dc;

    public Pilot(Drone drone) {
        super();
        this.drone = drone;
        dc = Fleet.getFleet().getCommanderForDrone(drone);
    }
}
