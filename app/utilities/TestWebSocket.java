package utilities;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.AltitudeChangedMessage;
import drones.messages.BatteryPercentageChangedMessage;
import drones.messages.LocationChangedMessage;
import models.Drone;
import play.Logger;
import play.libs.Json;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by matthias on 24/03/2015.
 */
public class TestWebSocket extends UntypedActor {

    public static Props props(ActorRef out) {
        return Props.create(TestWebSocket.class, out);
    }

    private final ActorRef out;

    private byte percentage = 100;
    private double altitude = 2.0;
    private double latitude = 3.5;
    private int notification = 0;

    public TestWebSocket(ActorRef out) {
        this.out = out;

        Drone testDroneEntity = Drone.FIND.all().get(0);

        System.out.println("brol");
        MessageGenerator generator1 = new MessageGenerator("batteryPercentageChanged",1,this.out,self(), 1000, (Void) -> {return getNewPercentageMessage();});
        new Thread(generator1).start();
        MessageGenerator generator2 = new MessageGenerator("altitudeChanged",1,this.out,self(), 750, (Void) -> {return getNewAlititudeMessage();});
        new Thread(generator2).start();
        MessageGenerator generator3 = new MessageGenerator("locationChanged",1,this.out,self(), 1000, (Void) -> {return getNewLocationMessage();});
        new Thread(generator3).start();
        MessageGenerator generator4 = new MessageGenerator("notification",1,this.out,self(), 3000, (Void) -> {return getNewNotificationMessage();});
        new Thread(generator4).start();

        out.tell("dit is een test bericht", self());
    }

    public Serializable getNewPercentageMessage() {
        percentage = (byte) (percentage == 0 ? 100 : percentage - 10);
        return new BatteryPercentageChangedMessage(percentage);
    }

    public Serializable getNewAlititudeMessage() {
        altitude = (altitude <= 0.1) ? 2.0 : (altitude - 0.1);
        return new AltitudeChangedMessage(altitude);
    }

    public Serializable getNewLocationMessage() {
        latitude+= 0.01;
        if (latitude > 180) {
            latitude = -180;
        }
        return new LocationChangedMessage(51.,latitude,1.);
    }

    public Serializable getNewNotificationMessage() {
        notification++;
        return new NotificationMessage("this is notification number " + notification);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        System.out.println(message);
    }

    public class NotificationMessage implements Serializable {
        private String message;

        public NotificationMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public class MessageGenerator implements Runnable {

        private String messageType;
        private int id;
        private ActorRef self;
        private ActorRef out;
        private Function<Void,Serializable> function;
        private int interval;

        public MessageGenerator(String messageType, int id, ActorRef out, ActorRef self, int interval, Function<Void,Serializable> function) {
            this.messageType = messageType;
            this.id = id;
            this.out = out;
            this.self = self;
            this.function = function;
            this.interval = interval;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    Serializable message = function.apply(null);

                    ObjectNode node = Json.newObject();
                    node.put("type", messageType);
                    node.put("id", id);
                    node.put("value", Json.toJson(message));
                    out.tell(node.toString(),self);
                    Thread.sleep(interval);
                } catch (Exception e) {
                    Logger.error(e.getMessage(),e);
                    self.tell(PoisonPill.getInstance(), self());
                }
            }
        }
    }
}
