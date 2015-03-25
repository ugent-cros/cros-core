package utilities;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.BatteryPercentageChangedMessage;
import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Drone;
import play.libs.Json;

import java.util.List;

/**
 * Created by matthias on 25/03/2015.
 */
public class MessageWebSocket extends AbstractActor {

    public static Props props(ActorRef out) {
        return Props.create(MessageWebSocket.class, out);
    }

    private final ActorRef out;

    public MessageWebSocket(ActorRef out) {
        this.out = out;

        List<Drone> drones = Drone.FIND.where().eq("name", "bepop").findList();
        Drone d = drones.get(drones.size()-1);
        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(d);
        commander.subscribeTopic(self(), BatteryPercentageChangedMessage.class);

        receive(ReceiveBuilder.match(BatteryPercentageChangedMessage.class,s -> {
            ObjectNode node = Json.newObject();
            node.put("type", "batteryPercentageChanged");
            node.put("id", d.getId());
            node.put("value", Json.toJson(s));
            out.tell(node.toString(), self());
        }).build());

    }

}
