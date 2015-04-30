package parrot.ardrone2;

import api.DroneDriver;
import model.DroneActor;
import api.DroneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by brecht on 3/24/15.
 */
public class ArDrone2Driver implements DroneDriver {

    public static final DroneType ARDRONE2_TYPE = new DroneType() {
        @Override
        public String getType() {
            return "ARDrone2";
        }

        @Override
        public String getVersionNumber() {
            return "ARDrone2";
        }
    };

    @Override
    public Set<DroneType> supportedTypes() {

        Set<DroneType> supportedTypes = new HashSet<>();
        supportedTypes.add(ARDRONE2_TYPE);
        return supportedTypes;
    }

    @Override
    public Class<ArDrone2> getActorClass() {
        return ArDrone2.class;
    }

    @Override
    public <T extends DroneActor> T createActor(String droneAddress) {
        // TODO: set indoor, hull property to true
        return (T) new ArDrone2(droneAddress, true, true);
    }
}