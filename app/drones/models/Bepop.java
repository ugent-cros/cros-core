package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import drones.commands.DroneCommandMessage;
import drones.commands.LandCommand;
import drones.messages.*;
import drones.commands.FlatTrimCommand;
import drones.commands.TakeOffCommand;
import drones.protocols.ArDrone3;
import drones.protocols.ArDrone3Discovery;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class Bepop extends Drone {

    private ActorRef discoveryProtocol;
    private ActorRef protocol;

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final String ip;
    private final boolean indoor;

    private final Object lock = new Object();

    private Promise<Boolean> initPromise;

    //TODO: use configuration class to pass here
    public Bepop(String ip, boolean indoor) {
        this.ip = ip;
        this.indoor = indoor;

        receive(ReceiveBuilder.
                        match(DroneDiscoveredMessage.class, this::handleDroneDiscoveryResponse).
                        match(PositionChangedMessage.class, s -> location.setValue(new Location(s.getLatitude(), s.getLongitude(), s.getGpsHeigth()))).
                        match(BatteryPercentageChangedMessage.class, s -> batteryPercentage.setValue(s.getPercent())).
                        match(FlyingStateChangedMessage.class, s -> status.setValue(s.getState())).
                        match(FlatTrimChangedMessage.class, s -> flatTrimStatus.setValue(true)).
                matchAny(o -> log.info("received unknown message")).build()
        );
    }

    private void handlePositionChangedMessage(PositionChangedMessage msg){
        location.setValue(new Location(msg.getLatitude() != PositionChangedMessage.UNAVAILABLE ? msg.getLatitude() : 0d,
                msg.getLongitude() != PositionChangedMessage.UNAVAILABLE ? msg.getLongitude() : 0d,
                msg.getGpsHeigth()));
    }

    private void handleDroneDiscoveryResponse(DroneDiscoveredMessage s){
        if (s.getStatus() == DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED) {
            initPromise.failure(new DroneException("Failed to get drone discovery response."));
        } else {
            setupDrone(s);
            initPromise.success(true);
        }
    }

    private <T extends Serializable> void sendMessage(T msg){
        if(protocol == null){
            log.warning("Trying to send message to uninitialized drone: [{}]", ip);
        } else {
            protocol.tell(new DroneCommandMessage<>(msg), self());
        }
    }

    private void setupDrone(final DroneDiscoveredMessage details) {
        // Assumes the drone is on the ground
        log.info("Discovery finished. Setting up protocol handlers");
        protocol = getContext().actorOf(Props.create(ArDrone3.class,
                () -> new ArDrone3(new DroneConnectionDetails(ip, details.getSendPort(), details.getRecvPort()), self())));

        // Send flattrim command
       // sendMessage(new FlatTrimCommand());
        //TODO: status request
    }

    @Override
    public Future<Boolean> init() {
        // Discovery protocol + setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = new Promise<>();
                discoveryProtocol = getContext().actorOf(Props.create(ArDrone3Discovery.class,
                        () -> new ArDrone3Discovery(ip, Bepop.this.self(), ArDrone3Discovery.DEFAULT_COMMAND_PORT)));
            }
        }
        return initPromise.future();
    }

    @Override
    public Future<Boolean> takeOff() {
        if(protocol == null){
            return Futures.failed(new DroneException("Drone is not initialized yet."));
        } else if(status != FlyingState.LANDED){
            return Futures.failed(new DroneException("Cannot takeoff from in-air situation."));
        } else {
            //TODO: only return when status changes to taking off promises
            sendMessage(new TakeOffCommand());
            return Futures.successful(true);
        }
    }

    @Override
    public Future<Boolean> land() {
        if(protocol == null){
            return Futures.failed(new DroneException("Drone is not initialized yet."));
        } else {
            //TODO: only return when status changes to landed
            sendMessage(new LandCommand());
            return Futures.successful(true);
        }
    }
}
