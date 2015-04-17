package drones.simulation;

import drones.models.DroneActor;
import drones.models.DroneDriver;
import drones.models.Location;
import models.DroneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by yasser on 25/03/15.
 */
public class SimulatorDriver implements DroneDriver {

    public static final DroneType SIMULATOR_TYPE = new DroneType("drones.simulation.DroneActorSimulator", "0.1");

    private static final Location STERRE = new Location(51.0226, 3.71, 0);

    // These properties can be changed any time
    // When the driver creates a simulator, these properties will be used in the simulator
    public Location startLocation = STERRE;
    public double maxHeight = 0;
    public double angleWrtEquator = Math.PI/2;  // Facing north by default
    public double topSpeed = 10;     // m/s

    @Override
    public Set<DroneType> supportedTypes() {
        Set<DroneType> supportedTypes = new HashSet<>();
        supportedTypes.add(SIMULATOR_TYPE);
        return supportedTypes;
    }

    @Override
    public <T extends DroneActor> Class<T> getActorClass() {
        return (Class<T>) BepopSimulator.class;
    }

    @Override
    public <T extends DroneActor> T createActor(String droneAddress) {
        return (T) new BepopSimulator(startLocation, maxHeight, angleWrtEquator, topSpeed);
    }
}
