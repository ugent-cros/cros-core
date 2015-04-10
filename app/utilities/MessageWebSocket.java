package utilities;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.AltitudeChangedMessage;
import drones.messages.BatteryPercentageChangedMessage;
import drones.messages.LocationChangedMessage;
import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Drone;
import play.libs.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by matthias on 25/03/2015.
 */
public class MessageWebSocket extends AbstractActor {

    public static Props props(ActorRef out) {
        return Props.create(MessageWebSocket.class, out);
    }

    private final ActorRef out;

    private static final Map<Class,String> TYPENAMES;

    static {
        TYPENAMES = new HashMap<>();

        TYPENAMES.put(BatteryPercentageChangedMessage.class,"batteryPercentageChanged");
        TYPENAMES.put(AltitudeChangedMessage.class,"altitudeChanged");
        TYPENAMES.put(LocationChangedMessage.class,"locationChanged");
    }

    public MessageWebSocket(ActorRef out) {
        this.out = out;

        List<Drone> drones = Drone.FIND.where().eq("name", "bepop").findList();
        Drone d = drones.get(drones.size()-1);
        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(d);
        TYPENAMES.keySet().forEach(c -> {
            commander.subscribeTopic(self(), c);

            receive(ReceiveBuilder.match(c, s -> {
                ObjectNode node = Json.newObject();
                node.put("type", TYPENAMES.get(c));
                node.put("id", d.getId());
                node.put("value", Json.toJson(s));
                out.tell(node.toString(), self());
            }).build());
        });



    }

}
