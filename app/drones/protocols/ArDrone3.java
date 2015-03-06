package drones.protocols;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.japi.Procedure;
import akka.util.ByteIterator;
import akka.util.ByteString;
import drones.models.DroneConnectionDetails;
import drones.models.Frame;
import drones.util.FrameHelper;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;

/**
 * Created by Cedric on 3/6/2015.
 */
public class ArDrone3 extends UntypedActor {

    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int MAX_FRAME_SIZE = 1500; //TODO check

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);


    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;
    private ActorRef connectionMgrRef;

    //http://doc.akka.io/docs/akka/2.2.0/java/io-codec.html
    private ByteString recvBuffer;

    public ArDrone3(DroneConnectionDetails details) {
        this.details = details;
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());

        final ActorRef mgr = Udp.get(getContext().system()).getManager();
        mgr.tell(
                UdpMessage.bind(getSelf(), new InetSocketAddress("localhost", details.getReceivingPort())),
                getSelf());
    }

    @Override
    public void preStart() {
        log.info("Starting ARDrone 3.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            this.connectionMgrRef = getSender();
            getContext().become(ready(connectionMgrRef));
            log.debug("Socket ARDRone 3.0 bound.");
        } else unhandled(msg);
    }

    private void sendData(ByteString data) {
        connectionMgrRef.tell(UdpMessage.send(data, senderAddress), getSelf());
    }

    private void processFrame(Frame frame) {
        log.debug("Processing frame: type = [{}], id = [{}], seq = [{}]", frame.getType(), frame.getId(), frame.getSeq());
    }

    private void processRawData(ByteString data) {
        ByteString current = recvBuffer == null ? data : recvBuffer.concat(data); //immutable rolling buffer

        while (true) {
            int len = current.length();
            if (len == 0) {
                recvBuffer = null;
                break;
            } else if (len < 7) {
                recvBuffer = current;
                break;
            } else {
                final int length = current.iterator().drop(3).getInt(BYTE_ORDER); //skip first 3 bytes (type, id, seq)
                if (length > MAX_FRAME_SIZE) {
                    log.error("Received too large frame: [{}]", length);
                    throw new IllegalArgumentException(
                            "received too large frame of size " + length + " (max = "
                                    + MAX_FRAME_SIZE + ")");
                } else if (current.length() < length) {
                    recvBuffer = current;
                    break;
                } else {

                    ByteIterator it = current.iterator();
                    final byte type = it.getByte();
                    final byte id = it.getByte();
                    final byte seq = it.getByte();

                    ByteString payload = current.slice(7, 7 + length);
                    processFrame(new Frame(FrameHelper.parseFrameType(type), id, seq, payload));

                    current = current.drop(length + 3 + 4); //TODO: check correctness (advance pointer with len + header)
                }
            }
        }

    }

    private Procedure<Object> ready(final ActorRef socket) {
        return msg -> {
            if (msg instanceof Udp.Received) {
                final Udp.Received r = (Udp.Received) msg;
                //socket.tell(UdpMessage.send(r.data(), r.sender()), getSelf());
                processRawData(r.data());
            } else if (msg.equals(UdpMessage.unbind())) {
                socket.tell(msg, getSelf());

            } else if (msg instanceof Udp.Unbound) {
                getContext().stop(getSelf());

            } else unhandled(msg);
        };
    }
}
