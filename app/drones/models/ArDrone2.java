package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.*;
import drones.messages.PingMessage;
import scala.concurrent.Promise;

import java.io.Serializable;

/**
 * Created by brecht on 3/9/15.
 */
public class ArDrone2 extends DroneActor {

    private ActorRef protocol;
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Promise<Void> initPromise;
    private final boolean indoor;
    private final String ip;
    private final Object lock = new Object();


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

                // @TODO change hard coded ports
                protocol = getContext().actorOf(Props.create(drones.protocols.ArDrone2.class,
                        () -> new drones.protocols.ArDrone2(new DroneConnectionDetails(ip, 5554, 5556), ArDrone2.this.self())));
            }
        }

        p.success(null);
    }

    private void handlePingResponse() {
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
                match(PingMessage.class, s -> handlePingResponse());
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
