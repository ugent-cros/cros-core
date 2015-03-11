package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.*;
import drones.messages.DroneDiscoveredMessage;
import drones.messages.StopMessage;
import drones.protocols.ArDrone3;
import drones.protocols.ArDrone3Discovery;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class Bepop extends DroneActor {

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
            protocol.tell(new StopMessage(), self()); // Stop the protocol (and bind)
            initPromise.failure(new DroneException("Failed to get drone discovery response."));
            initPromise = null;
        } else {
            setupDrone(s);

            initPromise.success(null);
        }
    }

    private <T extends Serializable> void sendMessage(T msg) {
        if(msg == null)
            return;

        if (protocol == null) {
            log.warning("Trying to send message to uninitialized drone: [{}]", ip);
        } else {
            protocol.tell(msg, self());
        }
    }

    private void setupDrone(final DroneDiscoveredMessage details) {
        // Assumes the drone is on the ground
        log.info("Discovery finished, forwarding connection details to protocl");
        protocol.tell(new DroneConnectionDetails(ip, details.getSendPort(), details.getRecvPort()), self()); //TODO: use forward here?


        sendMessage(new OutdoorCommand(!indoor));
        sendMessage(new RequestStatusCommand());
        sendMessage(new RequestSettingsCommand());
    }

    @Override
    protected void init(Promise<Void> p) {
        // Discovery protocol + setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = p;

                protocol = getContext().actorOf(Props.create(ArDrone3.class,
                        () -> new ArDrone3(ArDrone3Discovery.DEFAULT_COMMAND_PORT, Bepop.this.self()))); // Initialize listening already before broadcasting itself

                getContext().actorOf(Props.create(ArDrone3Discovery.class,
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
