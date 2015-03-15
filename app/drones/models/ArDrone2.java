package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import com.typesafe.config.ConfigException;
import drones.commands.*;
import drones.messages.DroneDiscoveredMessage;
import drones.messages.PingMessage;
import drones.messages.StopMessage;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by brecht on 3/9/15.
 */
public class ArDrone2 extends DroneActor {

    private ActorRef protocol;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final String ip;
    private final boolean indoor;
    private final Object lock = new Object();
    private Promise<Void> initPromise;

    public ArDrone2(String ip, boolean indoor) {
        this.ip = ip;
        this.indoor = indoor;
    }

    @Override
    protected void init(Promise<Void> p) {
        log.info("Starting ARDrone 2.0 INIT - In ACTOR");

        // Setup actor
        synchronized (lock) {
            if (initPromise == null) {
                initPromise = p;

                // @TODO
                protocol = getContext().actorOf(Props.create(drones.protocols.ArDrone2.class,
                        () -> new drones.protocols.ArDrone2(new DroneConnectionDetails(ip, 5554, 5556), ArDrone2.this.self())));
            }
        }

        p.success(null);
    }

    private void handlePingResponse(PingMessage s) {
        log.info("ArDrone Ping message received");
        setupDrone();
    }

    private void setupDrone() {
        log.info("Forwarding connection details to protocol");
        protocol.tell(new DroneConnectionDetails(ip, 5554, 5556), self());

        sendMessage(new OutdoorCommand(!indoor));
        sendMessage(new InitDroneCommand());
        sendMessage(new RequestSettingsCommand());
    }

    @Override
    protected void takeOff(Promise<Void> p) {
        sendMessage(new TakeOffCommand());
        p.success(null);
    }

    @Override
    protected void land(Promise<Void> p) {
        sendMessage(new LandCommand());
        p.success(null);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(PingMessage.class, this::handlePingResponse);
    }

    private <T extends Serializable> void sendMessage(T msg) {
        if (protocol == null) {
            log.warning("Trying to send message to uninitialized drone: [{}]", ip);
        } else {
            log.info("Message send [In ArDrone2]");
            protocol.tell(new DroneCommandMessage(msg), self());
        }
    }
}
