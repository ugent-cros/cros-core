package utilities.frontendSimulator;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.libs.Json;

import java.io.Serializable;

/**
 * Created by Benjamin on 14/04/2015.
 */
public class NotificationSimulator extends UntypedActor {

    public static Props props(ActorRef out) {
        return Props.create(NotificationSimulator.class, out);
    }
    private final ActorRef out;
    private SchedulerSimulator sheduler = null;
    private Thread thread = null;

    public NotificationSimulator(ActorRef out) {
        this.out = out;

        sheduler = new SchedulerSimulator(this);
        thread = new Thread(sheduler);
        thread.start();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        System.out.println(message);
    }

    public void sendMessage(String messageType, long id, Serializable message) {
        try {
            ObjectNode node = Json.newObject();
            node.put("type", messageType);
            node.put("id", id);
            node.put("value", Json.toJson(message));
            out.tell(node.toString(),self());
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
            // cascade terminate
            self().tell(PoisonPill.getInstance(), self());
        }
    }
}
