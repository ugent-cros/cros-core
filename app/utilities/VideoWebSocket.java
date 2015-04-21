package utilities;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.JPEGFrameMessage;
import drones.models.Fleet;
import play.Logger;
import play.libs.Json;

/**
 * Created by brecht on 4/20/15.
 */
public class VideoWebSocket extends AbstractActor {

    public static Props props(ActorRef out) {
        return Props.create(VideoWebSocket.class, out);
    }

    private final ActorRef out;

    public VideoWebSocket(final ActorRef out) {
        this.out = out;

        ReceiveBuilder.match(JPEGFrameMessage.class, s -> handleJPEGFrameMessage(s))
                .matchAny(s -> Logger.debug("[videosocket] unkown message type..."));;

    }

    private void handleJPEGFrameMessage(JPEGFrameMessage s) {
        ObjectNode node = Json.newObject();
        node.put("type", "JPEGFrameChanged");
        node.put("id", sender().path().name().split("-")[1]);
        node.put("value", Json.toJson(s));

        out.tell(node.toString(), self());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        Fleet.getFleet().unsubscribe(self());
    }
}
