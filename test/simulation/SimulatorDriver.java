package simulation;

import drones.models.DroneActor;
import drones.models.DroneDriver;
import models.Drone;
import models.DroneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by yasser on 25/03/15.
 */
public class SimulatorDriver implements DroneDriver {

    public static final DroneType SIMULATOR_TYPE = new DroneType("simulation.DroneActorSimulator", "0.1");

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
    public <T extends DroneActor> T createActor(Drone droneEntity) {
        return (T) new BepopSimulator();
    }
}
