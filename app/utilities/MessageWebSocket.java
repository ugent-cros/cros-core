package utilities;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.BatteryPercentageChangedMessage;
import play.libs.Json;

/**
 * Created by matthias on 24/03/2015.
 */
public class MessageWebSocket extends UntypedActor {

    public static Props props(ActorRef out) {
        return Props.create(MessageWebSocket.class, out);
    }

    private final ActorRef out;

    public MessageWebSocket(ActorRef out) {
        this.out = out;

        /*Drone testDroneEntity = Drone.FIND.byId((long) 1);
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(testDroneEntity);
        d.subscribeTopic(out, BatteryPercentageChangedMessage.class);*/
        byte percentage = 100;
        while(true) {
            BatteryPercentageChangedMessage message = new BatteryPercentageChangedMessage(percentage);
            // TODO: fix adding root element
            ObjectNode node = Json.newObject();
            node.put("type", "batteryPercentageChanged");
            node.put("value", Json.toJson(message));
            try {
                out.tell(node.toString(), self());
                Thread.sleep(500);
            } catch (Exception e) {
                //e.printStackTrace();
                break;
            }
            percentage = (byte) (percentage == 0 ? 100 : percentage - 10);
        }

        self().tell(PoisonPill.getInstance(), self());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        System.out.println(message);
    }
}
