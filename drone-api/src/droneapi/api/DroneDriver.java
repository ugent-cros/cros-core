package droneapi.api;

import droneapi.model.DroneActor;

import java.util.Set;

/**
 * Created by yasser on 17/03/15.
 */
public interface DroneDriver {

    public Set<DroneType> supportedTypes();
    public <T extends DroneActor> Class<T> getActorClass();
    public <T extends DroneActor> T createActor(String address);
}
