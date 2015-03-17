package drones.models;

import models.Drone;

/**
 * Created by yasser on 17/03/15.
 */
public interface DroneActorFactory {

    public <T extends DroneActor> Class<T> getActorClass();
    public <T extends DroneActor> T createActor(Drone droneEntity);
}
