package drones.ardrone3;

import api.DroneDriver;
import drones.ardrone3.Bepop;
import model.DroneActor;
import api.DroneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by yasser on 17/03/15.
 */
public class BepopDriver implements DroneDriver {

    public static final DroneType BEPOP_TYPE = new DroneType() {
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
        supportedTypes.add(BEPOP_TYPE);
        return supportedTypes;
    }

    @Override
    public Class<Bepop> getActorClass() {
        return Bepop.class;
    }

    @Override
    public <T extends DroneActor> T createActor(String droneAddress) {
        // TODO: set indoor, hull property to true
        return (T) new Bepop(nextd2cPort++, droneAddress, true, true);
    }
}
