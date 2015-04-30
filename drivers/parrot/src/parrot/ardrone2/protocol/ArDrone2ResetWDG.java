package parrot.ardrone2.protocol;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import parrot.ardrone2.util.DefaultPorts;
import parrot.ardrone2.commands.ATCommandCOMWDG;
import parrot.shared.models.DroneConnectionDetails;
import parrot.ardrone2.util.PacketCreator;
import messages.StopMessage;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by brecht on 3/23/15.
 */
public class ArDrone2ResetWDG extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;
    private final ActorRef udpManager;
    private InetSocketAddress senderAddressATC;

    private ByteString data = PacketCreator.createPacket(new ATCommandCOMWDG(1));

    public ArDrone2ResetWDG(DroneConnectionDetails details) {
        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), new InetSocketAddress(0)), getSelf());

        this.senderAddressATC = new InetSocketAddress(details.getIp(), DefaultPorts.AT_COMMAND.getPort());

        // Request a sender socket
        udpManager.tell(UdpMessage.simpleSender(), getSelf());
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Udp.Bound) {
            log.info("[ARDRONE2COMWDG] Socket ARDRone 2.0 bound.");

            senderRef = sender();
            // Setup handlers
            getContext().become(ReceiveBuilder
                    .match(Udp.Unbound.class, s -> getContext().stop(getSelf()))
                    .match(Udp.SimpleSenderReady.class, s -> senderRef = sender())
                    .match(StopMessage.class, s -> stop())
                    .match(ComWDGMessage.class, s -> sendComWDG())
                    .matchAny(s -> {
                        log.warning("[ARDRONE2COMWDG] No protocol handler for [{}]", s.getClass().getCanonicalName());
                        unhandled(s);
                    })
                    .build());

            runComWDG();
        } else if(msg instanceof Udp.SimpleSenderReady){
            senderRef = sender();
        } else {
            log.error("[ARDRONE2COMWDG] Unhandled message received - ArDrone2ResetWDG protocol");
            unhandled(msg);
        }
    }

    private void stop() {
        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }

    private void sendComWDG() {
        if (senderAddressATC != null && senderRef != null) {
            senderRef.tell(UdpMessage.send(data, senderAddressATC), getSelf());
        } else {
            if(senderAddressATC == null) log.info("ATC null");
            if(senderRef == null) log.info("Ref null");
            log.error("[ARDRONE2COMWDG] Sending data failed (senderAddressATC or senderRef is null).");
        }
    }

    private void runComWDG() {
        getContext().system().scheduler().schedule(Duration.create(0, TimeUnit.MILLISECONDS), Duration.create(50, TimeUnit.MILLISECONDS),
                getSelf(), new ComWDGMessage(), getContext().system().dispatcher(), null);
    }

    private class ComWDGMessage implements Serializable {}
}
