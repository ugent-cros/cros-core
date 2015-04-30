package parrot.ardrone3;

import droneapi.api.DroneDriver;
import droneapi.model.DroneActor;
import droneapi.api.DroneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by yasser on 17/03/15.
 */
public class BebopDriver implements DroneDriver {

    public static final DroneType BEBOP_TYPE = new DroneType() {
        @Override
        public String getType() {
            return "ARDrone3";
        }

        @Override
        public String getVersionNumber() {
            return "Bebop";
        }
    };

    private int nextd2cPort = 54321;

    @Override
    public Set<DroneType> supportedTypes() {

        Set<DroneType> supportedTypes = new HashSet<>();
        supportedTypes.add(BEBOP_TYPE);
        return supportedTypes;
    }

    @Override
    public Class<Bebop> getActorClass() {
        return Bebop.class;
    }

    @Override
    public <T extends DroneActor> T createActor(String droneAddress) {
        // TODO: set indoor, hull property to true
        return (T) new Bebop(nextd2cPort++, droneAddress, true, true);
    }
}
