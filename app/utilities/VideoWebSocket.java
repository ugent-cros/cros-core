package utilities;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import messages.ImageMessage;
import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Drone;
import play.libs.Json;

import java.util.Base64;

/**
 * Created by brecht on 4/20/15.
 */
public class VideoWebSocket extends AbstractActor {
    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props(ActorRef out, long droneID) {
        return Props.create(VideoWebSocket.class, out, droneID);
    }

    private final ActorRef out;

    public VideoWebSocket(final ActorRef out, long droneID) {
        this.out = out;

        Drone drone = Drone.FIND.byId(droneID);
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        d.subscribeTopic(self(), ImageMessage.class);

        receive(ReceiveBuilder.match(ImageMessage.class, s -> handleJPEGFrameMessage(s)).build());

        log.info("[VIDEOSOCKET] Started (for ID: {})", droneID);
    }

    private void handleJPEGFrameMessage(ImageMessage s) {
        ObjectNode node = Json.newObject();
        node.put("type", "JPEGFrameChanged");
        node.put("id", sender().path().name().split("-")[1]);

        node.put("value", Json.toJson(new JPEGBase64Image(s.getByteData())));

        out.tell(node.toString(), self());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        Fleet.getFleet().unsubscribe(self());
    }

    private class JPEGBase64Image {
        private String imageData;

        public JPEGBase64Image(byte[] data) {
            this.imageData = Base64.getEncoder().encodeToString(data);
        }

        public String getImageData() {
            return imageData;
        }
    }
}
