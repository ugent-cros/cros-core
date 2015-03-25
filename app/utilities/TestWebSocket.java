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
import play.libs.Json;

import java.io.Serializable;

/**
 * Created by matthias on 24/03/2015.
 */
public class TestWebSocket extends UntypedActor {

    public static Props props(ActorRef out) {
        return Props.create(TestWebSocket.class, out);
    }

    private final ActorRef out;

    public TestWebSocket(ActorRef out) {
        this.out = out;

        Drone testDroneEntity = Drone.FIND.all().get(0);
        /*DroneCommander d = Fleet.getFleet().getCommanderForDrone(testDroneEntity);
        d.subscribeTopic(out, BatteryPercentageChangedMessage.class);*/
        byte percentage = 100;
        double altitude = 1.0;
        double longitude = 1.0;
        byte amoutOfTypes = 4;
        int notification = 1;
        byte currentType = 0;
        while(true) {
            Serializable message = null;
            switch (currentType) {
                case 0:
                    message = new BatteryPercentageChangedMessage(percentage);
                    percentage = (byte) (percentage == 0 ? 100 : percentage - 10);
                    break;
                case 1:
                    message = new AltitudeChangedMessage(altitude);
                    altitude = 1.0 - altitude;
                    break;
                case 2:
                    message = new LocationChangedMessage(longitude,5.,1.);
                    longitude = 1.0 - longitude;
                    break;
                case 3:
                    message = new NotificationMessage("this is notification number " + notification);
                    break;
            }

            String type = "";
            switch (currentType) {
                case 0:
                    type = "batteryPercentageChanged";
                    break;
                case 1:
                    type = "altitudeChanged";
                    break;
                case 2:
                    type = "locationChanged";
                    break;
                case 3:
                    type = "notification";
                    break;
            }
            // TODO: fix adding root element
            ObjectNode node = Json.newObject();
            node.put("type", type);
            node.put("id", testDroneEntity.getId());
            node.put("value", Json.toJson(message));
            try {
                out.tell(node.toString(), self());
                Thread.sleep(500);
            } catch (Exception e) {
                //e.printStackTrace();
                break;
            }
            currentType = (byte) ((currentType + 1) % amoutOfTypes);
        }

        self().tell(PoisonPill.getInstance(), self());
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
}
