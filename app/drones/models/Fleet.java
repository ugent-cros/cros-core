package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnFailure;
import akka.pattern.Patterns;
import akka.util.Timeout;
import droneapi.api.DroneCommander;
import droneapi.api.DroneDriver;
import droneapi.messages.*;
import models.Drone;
import models.DroneType;
import parrot.ardrone2.ArDrone2Driver;
import parrot.ardrone3.BebopDriver;
import parrot.messages.PingMessage;
import parrot.shared.models.PingResult;
import parrot.shared.protocols.ICMPPing;
import play.libs.Akka;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import simulator.SimulatorDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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

        BebopDriver bebopDriver = new BebopDriver();
        registerDriver(new DroneType(BebopDriver.BEBOP_TYPE), bebopDriver);

        ArDrone2Driver ardrone2Driver = new ArDrone2Driver();
        registerDriver(new DroneType(ArDrone2Driver.ARDRONE2_TYPE), ardrone2Driver);

        SimulatorDriver simulatorDriver = new SimulatorDriver();
        registerDriver(new DroneType(SimulatorDriver.SIMULATOR_TYPE), simulatorDriver);
        // TODO: do this dynamically by scanning all classes extending DroneActor for factory property
    }

    /* Singleton */

    private static final Fleet fleet = new Fleet();

    public static Fleet getFleet() {
        return fleet;
    }


    /* Instance */

    private ConcurrentMap<Long, DroneCommander> drones;

    private ActorRef fleetBus;

    public Fleet() {
        drones = new ConcurrentHashMap<>();
        fleetBus =  Akka.system().actorOf(Props.create(BroadcastBus.class), "fleetbus");
    }

    private ActorRef pinger;

    public Future<PingResult> isReachable(Drone droneEntity) {
        // If the entity has no commander, fail immediately
        if(!hasCommander(droneEntity)){
            return Futures.failed(new IllegalArgumentException("Drone is not initialized yet."));
        }
        // If the drone is simulated, pretend to succeed immediately
        if(SimulatorDriver.SIMULATOR_TYPE.equals(droneEntity.getDroneType())){
            return Futures.successful(PingResult.OK);
        }

        // Lazy load the ping class
        if (pinger == null) {
            pinger = Akka.system().actorOf(Props.create(ICMPPing.class), "pinger");
        }

        return Patterns.ask(pinger, new PingMessage(droneEntity.getAddress()),
                new Timeout(Duration.create(ICMPPing.PING_TIMEOUT + 1000, TimeUnit.MILLISECONDS)))
                .map(new Mapper<Object, PingResult>() {
                    public PingResult apply(Object s) {
                        return (PingResult) s;
                    }
                }, Akka.system().dispatcher());
    }

    private void registerFleetBus(DroneCommander cmd){
        cmd.subscribeTopics(fleetBus, new Class[]{
                LocationChangedMessage.class,
                BatteryPercentageChangedMessage.class,
                ConnectionStatusChangedMessage.class,
                FlyingStateChangedMessage.class,
                NavigationStateChangedMessage.class,
                AltitudeChangedMessage.class
        });
    }

    /**
     * Subscribe to all messages of the fleet
     * @param actor The actor to forward all messages to
     */
    public void subscribe(final ActorRef actor){
        fleetBus.tell(new SubscribeEventMessage(), actor);
    }

    /**
     * Unsubscribe to all messages of the fleet
     * @param actor The actor to unsubscribe
     */
    public void unsubscribe(final ActorRef actor){
        fleetBus.tell(new UnsubscribeEventMessage(), actor);
    }

    public Future<DroneCommander> createCommanderForDrone(Drone droneEntity) {
        DroneDriver driver = getDriver(droneEntity.getDroneType());
        if (driver == null)
            return null;


        // Create commander
        ActorRef droneActor = Akka.system().actorOf(
                Props.create(driver.getActorClass(),
                        () -> driver.createActor(droneEntity.getAddress())), String.format("droneactor-%d", droneEntity.getId()));
        DroneCommander commander = new DroneCommander(droneActor);
        Future<Void> f = commander.init();
        f.onFailure(new OnFailure() {
            @Override
            public void onFailure(Throwable failure) throws Throwable {
                commander.stop(); // Stop commander when init fails
            }
        }, Akka.system().dispatcher());
        return f.map(new Mapper<Void, DroneCommander>() {
            public DroneCommander apply(Void s) {
                registerFleetBus(commander);
                drones.put(droneEntity.getId(), commander);
                return commander;
            }
        }, Akka.system().dispatcher());
    }

    public void shutdown(){
        for(DroneCommander cmd : drones.values()){
            cmd.stop();
        }
        drones.clear();
    }

    public boolean stopCommander(Drone droneEntity){
        DroneCommander cmd = drones.remove(droneEntity.getId());
        if(cmd != null){
            cmd.stop();
            return true;
        } else return false;
    }

    public boolean hasCommander(Drone droneEntity) {
        return drones.containsKey(droneEntity.getId());
    }

    public DroneCommander getCommanderForDrone(Drone droneEntity) {
        DroneCommander commander = drones.get(droneEntity.getId());
        if (commander == null) {
            throw new IllegalArgumentException("Drone is not initialized yet. Use createCommander first.");
        }
        return commander;
    }
}
