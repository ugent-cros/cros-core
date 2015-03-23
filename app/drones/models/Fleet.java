package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import models.Drone;
import models.DroneType;
import play.libs.Akka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Yasser.
 */
public class Fleet {

    /* Supporting multiple drone types */
    private static ConcurrentMap<DroneType, DroneDriver> drivers = new ConcurrentHashMap<>();

    public static void registerDriver(DroneType droneType, DroneDriver factory) {
        drivers.put(droneType, factory);
    }

    // Removes the driver if it's currently associated with the given type
    public static boolean unregisterDriver(DroneType droneType, DroneDriver driver) {
        return drivers.remove(droneType, driver);
    }

    // Returns a copy of the registered drivers
    public static Map<DroneType, DroneDriver> registeredDrivers() {
        return new HashMap<>(drivers);
    }

    private static DroneDriver getDriver(DroneType droneType) {
        return drivers.get(droneType);
    }

    static {

        BepopDriver bepopDriver = new BepopDriver();
        registerDriver(BepopDriver.BEPOP_TYPE, bepopDriver);

        // TODO: do this dynamically by scanning all classes extending DroneActor for factory property
    }

    /* Singleton */

    private static final Fleet fleet = new Fleet();

    public static Fleet getFleet(){
        return fleet;
    }


    /* Instance */

    private ConcurrentMap<Drone, DroneCommander> drones;

    public Fleet(){
        drones = new ConcurrentHashMap<>();
    }

    public DroneCommander getCommanderForDrone(Drone droneEntity) {

        DroneCommander commander = drones.get(droneEntity);

        // If commander does not exist yet, create it
        if (commander == null) {

            // Get the driver, if available
            DroneDriver driver = getDriver(droneEntity.getDroneType());
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
