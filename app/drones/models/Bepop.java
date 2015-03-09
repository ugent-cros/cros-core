package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.*;
import drones.messages.*;
import drones.protocols.ArDrone3;
import drones.protocols.ArDrone3Discovery;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class Bepop extends DroneActor {

    private ActorRef discoveryProtocol;
    private ActorRef protocol;

    private final String ip;
    private final boolean indoor;

    private final Object lock = new Object();

    private Promise<Void> initPromise;

    //TODO: use configuration class to pass here
    public Bepop(String ip, boolean indoor) {
        this.ip = ip;
        this.indoor = indoor;
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(DroneDiscoveredMessage.class, this::handleDroneDiscoveryResponse);
    }

    private void handleDroneDiscoveryResponse(DroneDiscoveredMessage s) {
        if (s.getStatus() == DroneDiscoveredMessage.DroneDiscoveryStatus.FAILED) {
            initPromise.failure(new DroneException("Failed to get drone discovery response."));
        } else {
            setupDrone(s);

            initPromise.success(null);
        }
    }

    private <T extends Serializable> void sendMessage(T msg) {
        if (protocol == null) {
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

        sendMessage(new RequestStatusCommand());
        sendMessage(new OutdoorCommand(!indoor));
    }

    @Override
    protected void init(Promise<Void> p) {
        // Discovery protocol + setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = p;
                discoveryProtocol = getContext().actorOf(Props.create(ArDrone3Discovery.class,
                        () -> new ArDrone3Discovery(ip, Bepop.this.self(), ArDrone3Discovery.DEFAULT_COMMAND_PORT)));
            }
        }
    }


    @Override
    protected void takeOff(Promise<Void> p) {
        //TODO: only return when status changes to taking off promises
        sendMessage(new FlatTrimCommand());
        sendMessage(new TakeOffCommand());
        p.success(null);
    }

    @Override
    protected void land(Promise<Void> p) {
        sendMessage(new LandCommand());
        p.success(null);
    }
}
