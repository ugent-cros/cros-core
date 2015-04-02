package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Mapper;
import akka.util.Timeout;
import drones.messages.PingMessage;
import drones.protocols.ICMPPing;
import drones.simulation.SimulatorDriver;
import models.Drone;
import models.DroneType;
import play.libs.Akka;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

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

        ArDrone2Driver ardrone2Driver = new ArDrone2Driver();
        registerDriver(ArDrone2Driver.ARDRONE2_TYPE, ardrone2Driver);

        SimulatorDriver simulatorDriver = new SimulatorDriver();
        registerDriver(SimulatorDriver.SIMULATOR_TYPE, simulatorDriver);
        // TODO: do this dynamically by scanning all classes extending DroneActor for factory property
    }

    /* Singleton */

    private static final Fleet fleet = new Fleet();

    public static Fleet getFleet(){
        return fleet;
    }


    /* Instance */

    private ConcurrentMap<Long, DroneCommander> drones;

    public Fleet(){
        drones = new ConcurrentHashMap<>();
    }

    private ActorRef pinger;

    public Future<PingResult> isReachable(Drone droneEntity){
        // Lazy load the ping class
        if(pinger == null){
            pinger = Akka.system().actorOf(Props.create(ICMPPing.class), "pinger");
        }

        return ask(pinger, new PingMessage(droneEntity.getAddress()),
                new Timeout(Duration.create(ICMPPing.PING_TIMEOUT + 1000, TimeUnit.MILLISECONDS)))
                .map(new Mapper<Object, PingResult>() {
                    public PingResult apply(Object s) {
                        return (PingResult)s;
                    }
                }, Akka.system().dispatcher());
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
            drones.put(droneEntity.getId(), commander);
        }

        return commander;
    }
}
