package drones.models;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.commands.DroneCommandMessage;
import drones.commands.EmergencyCommand;
import drones.commands.LandCommand;
import drones.commands.TakeOffCommand;
import drones.messages.BatteryPercentageChangedMessage;
import drones.messages.FlyingStateChangedMessage;
import drones.messages.PositionChangedMessage;
import scala.concurrent.Future;
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

    private Promise<Boolean> initPromise;

    public ArDrone2(String ip, boolean indoor) {
        this.ip = ip;
        this.indoor = indoor;
    }

    private void handlePositionChangedMessage(PositionChangedMessage msg){
        location.setValue(new Location(msg.getLatitude() != PositionChangedMessage.UNAVAILABLE ? msg.getLatitude() : 0d,
                msg.getLongitude() != PositionChangedMessage.UNAVAILABLE ? msg.getLongitude() : 0d,
                msg.getGpsHeigth()));
    }

    private <T extends Serializable> void sendMessage(T msg) {
        if (protocol == null) {
            log.warning("Trying to send message to uninitialized drone: [{}]", ip);
        } else {
            protocol.tell(new DroneCommandMessage<>(msg), self());
        }
    }

    @Override
    protected void init(Promise<Void> p) {

    }

    @Override
    protected void takeOff(Promise<Void> p) {
        // @TODO
        sendMessage(new TakeOffCommand());
        p.success(null);
    }

    @Override
    protected void land(Promise<Void> p) {
        // @TODO
        sendMessage(new LandCommand());
        p.success(null);
    }

    @Override
    protected void emergency(Promise<Void> p) {
        // @TODO
        sendMessage(new EmergencyCommand());
        p.success(null);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return null;
    }
}
