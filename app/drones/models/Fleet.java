package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import models.Drone;
import play.libs.Akka;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Cedric on 3/9/2015.
 */
public class Fleet {

    /* Supporting multiple drone types */
    private static Map<String, DroneActorFactory> drivers = new HashMap<>();

    // This method will override any existing drivers for the given droneType
    public static void registerDriver(String droneType, DroneActorFactory factory) {
        drivers.put(droneType, factory);
    }

    public static DroneActorFactory unregisterDriver(String droneType) {
        return drivers.remove(droneType);
    }

    private static DroneActorFactory getDriver(String droneType) {
        return drivers.get(droneType);
    }

    static {

        DroneActorFactory bepopFactory = new DroneActorFactory() {
            @Override
            public <T extends DroneActor> Class<T> getActorClass() {
                return (Class<T>) Bepop.class;
            }

            @Override
            public <T extends DroneActor> T createActor(Drone droneEntity) {
                // TODO: find solution for fixed indoor parameter
                return (T) new Bepop(droneEntity.getAddress(), true);
            }
        };

        registerDriver(Drone.CommunicationType.DEFAULT.name(), bepopFactory);

        // TODO: do this dynamically by scanning all classes extending DroneActor for factory property
    }

    // TODO: replace communitcationType in drone by String or Type object
    // Type object: type + version?

    /* Singleton */

    private static final Fleet fleet = new Fleet();

    public static Fleet getFleet(){
        return fleet;
    }


    /* Instance */

    private Map<Drone, DroneCommander> drones;

    public Fleet(){
        drones = new HashMap<>();
    }

    public DroneCommander getCommanderForDrone(Drone droneEntity) {

        DroneCommander commander = drones.get(droneEntity);

        // If commander does not exist yet, create it
        if (commander == null) {

            // Get the driver, if available
            DroneActorFactory driver = getDriver(droneEntity.getCommunicationType().name());
            if (driver == null)
                return null;

            // Create commander
            ActorRef ref = Akka.system().actorOf(
                    Props.create(driver.getActorClass(),
                            () -> driver.createActor(droneEntity)));
            commander = new DroneCommander(ref);
            drones.put(droneEntity, commander);
        }

        return commander;
    }
}
