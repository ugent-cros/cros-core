package utilities;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.*;
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
    private static final List<Class> IGNORETYPES;

    static {
        TYPENAMES = new ArrayList<>();
        IGNORETYPES = new ArrayList<>();

        TYPENAMES.add(new F.Tuple<>(BatteryPercentageChangedMessage.class, "batteryPercentageChanged"));
        TYPENAMES.add(new F.Tuple<>(AltitudeChangedMessage.class, "altitudeChanged"));
        TYPENAMES.add(new F.Tuple<>(LocationChangedMessage.class, "locationChanged"));
        TYPENAMES.add(new F.Tuple<>(DroneAssignedMessage.class, "droneAssigned"));
        TYPENAMES.add(new F.Tuple<>(AssignmentStartedMessage.class, "assignmentStarted"));
        TYPENAMES.add(new F.Tuple<>(AssignmentProgressChangedMessage.class, "assignmentProgressChanged"));
        TYPENAMES.add(new F.Tuple<>(AssignmentCompletedMessage.class, "assignmentCompleted"));

        IGNORETYPES.add(FlyingStateChangedMessage.class);
        IGNORETYPES.add(NavigationStateChangedMessage.class);
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

        for (int i = 0; i < IGNORETYPES.size(); ++i)
            builder = builder.match(IGNORETYPES.get(i), s -> {});

        builder = builder.matchAny(o -> Logger.debug("[websocket] Unkown message type..." + o.getClass().getName()));

        receive(builder.build());

    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        Fleet.getFleet().unsubscribe(self());
    }
}
