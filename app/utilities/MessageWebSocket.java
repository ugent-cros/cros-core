package utilities;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.AltitudeChangedMessage;
import drones.messages.BatteryPercentageChangedMessage;
import drones.messages.LocationChangedMessage;
import drones.models.Fleet;
import play.Logger;
import play.libs.F;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthias on 25/03/2015.
 */
public class MessageWebSocket extends AbstractActor {

    public static Props props(ActorRef out) {
        return Props.create(MessageWebSocket.class, out);
    }

    private final ActorRef out;

    private static final List<F.Tuple<Class, String>> TYPENAMES;

    static {
        TYPENAMES = new ArrayList<>();

        TYPENAMES.add(new F.Tuple<>(BatteryPercentageChangedMessage.class, "batteryPercentageChanged"));
        TYPENAMES.add(new F.Tuple<>(AltitudeChangedMessage.class, "altitudeChanged"));
        TYPENAMES.add(new F.Tuple<>(LocationChangedMessage.class, "locationChanged"));
    }

    public MessageWebSocket(final ActorRef out) {
        this.out = out;

        Fleet.getFleet().subscribe(self());
        UnitPFBuilder<Object> builder = ReceiveBuilder.match(TYPENAMES.get(0)._1, s -> {
            ObjectNode node = Json.newObject();
            node.put("type", TYPENAMES.get(0)._2);
            node.put("id", sender().path().name().split("-")[1]); // todo: set to correct id
            node.put("value", Json.toJson(s));
            out.tell(node.toString(), self());
        });

        for(int i = 1; i < TYPENAMES.size(); i++){
            final int index = i;
            builder = builder.match(TYPENAMES.get(index)._1, s -> {
                ObjectNode node = Json.newObject();
                node.put("type", TYPENAMES.get(index)._2);
                node.put("id", sender().path().name().split("-")[1]); // todo: set to correct id
                node.put("value", Json.toJson(s));
                out.tell(node.toString(), self());
            });
        }

        builder = builder.matchAny(o -> Logger.debug("[websocket] unkown message type..."));

        receive(builder.build());

    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        Fleet.getFleet().unsubscribe(self());
    }
}
