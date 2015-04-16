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
import play.Logger;
import play.libs.Json;

import java.util.HashMap;
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

        TYPENAMES.put(BatteryPercentageChangedMessage.class, "batteryPercentageChanged");
        TYPENAMES.put(AltitudeChangedMessage.class, "altitudeChanged");
        TYPENAMES.put(LocationChangedMessage.class, "locationChanged");
    }

    public MessageWebSocket(ActorRef out) {
        this.out = out;
        ReceiveBuilder builder = null;

        TYPENAMES.keySet().stream().map(messageType -> ReceiveBuilder.match(messageType, s -> {
            ObjectNode node = Json.newObject();
            node.put("type", TYPENAMES.get(messageType));
            node.put("id", 0); // todo: set to correct id
            node.put("value", Json.toJson(s));
            out.tell(node.toString(), self());
        })).reduce((builder1,builder2) -> { builder1.match(builder2); });

        Drone.FIND.all().forEach(d -> {
            try {
                DroneCommander commander = Fleet.getFleet().getCommanderForDrone(d);
                TYPENAMES.keySet().forEach(c -> {
                    commander.subscribeTopic(self(), c);

                    if (builder == null) {
                        builder = ReceiveBuilder.match(c, s -> {
                            ObjectNode node = Json.newObject();
                            node.put("type", TYPENAMES.get(c));
                            node.put("id", d.getId());
                            node.put("value", Json.toJson(s));
                            out.tell(node.toString(), self());
                        });
                    } else {
                        builder = builder.match()
                    }
                });
            } catch (IllegalArgumentException e) {
                Logger.error("un-initialized drones!!!!", e);
            }
        });

        receive(ReceiveBuilder.matchAny(c))
    }

}
