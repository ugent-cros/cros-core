package parrot.ardrone2.protocol;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.*;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteIterator;
import akka.util.ByteString;
import droneapi.messages.StopMessage;
import parrot.ardrone2.util.DefaultPorts;
import parrot.shared.models.DroneConnectionDetails;
import parrot.shared.protocols.H264Decoder;

import java.io.*;
import java.net.InetSocketAddress;

/**
 * Created by brecht on 4/17/15.
 */
public class ArDrone2Video extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorRef tcpManager;
    private InetSocketAddress senderAddressVideo;

    private static final int MAX_INPUT_SIZE = 4999999;

    private static PipedInputStream pis;
    private static PipedOutputStream pos;

    public ArDrone2Video(DroneConnectionDetails details, final ActorRef listener, final ActorRef parent) {
        this.senderAddressVideo = new InetSocketAddress(details.getIp(), DefaultPorts.VIDEO_DATA.getPort());

        // TCP manager
        tcpManager = Tcp.get(getContext().system()).manager();
        tcpManager.tell(TcpMessage.connect(senderAddressVideo), getSelf());

        try {
            pos = new PipedOutputStream();
            pis = new PipedInputStream(pos, MAX_INPUT_SIZE);

            H264Decoder decoder = new H264Decoder(pis, listener);
            decoder.start();
        } catch (IOException e) {
            log.error(e, "Error in video stream");
        }
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        //log.error("[ARDRONE2VIDEO] Error");
        if (msg instanceof Tcp.CommandFailed) {
            log.error("[ARDRONE2VIDEO] TCP command failed");
            getContext().stop(getSelf());
        } else if (msg instanceof Tcp.Connected) {
            log.info("[ARDRONE2VIDEO] Connected to {}", senderAddressVideo);

            getSender().tell(TcpMessage.register(getSelf()), getSelf());
            getContext().become(ReceiveBuilder
                    .match(Tcp.Received.class, b -> processRawData(b.data()))
                    .match(Tcp.CommandFailed.class, m -> commandFailed())
                    .match(StopMessage.class, m -> getContext().stop(getSelf()))
                    .match(Tcp.ConnectionClosed.class, m -> connectionClosed()).build());
        } else if(msg instanceof StopMessage){
            getContext().stop(getSelf());
        }
    }

    private void processRawData(ByteString data) {
        byte[] received = new byte[data.length()];
        ByteIterator it = data.iterator();

        int i = 0;
        while(it.hasNext()) {
            received[i] = it.getByte();
            i++;
        }

        try {
            pos.write(received, 0, received.length);
            pos.flush();
        } catch (IOException ex) {
            log.error(ex, "Exception in processing ArDrone2 video");
        }
    }

    private void connectionClosed() {
        log.info("[ARDRONE2VIDEO] TCP connection closed");
        getContext().stop(getSelf());
    }

    private void commandFailed() {
        log.error("[ARDRONE2VIDEO] TCP command failed");
        getContext().stop(getSelf());
    }
}
