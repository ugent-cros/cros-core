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
import drones.commands.*;
import drones.commands.ardrone2.atcommand.*;
import drones.messages.StopMessage;
import drones.models.DroneConnectionDetails;
import drones.util.ardrone2.PacketCreator;

import java.net.InetSocketAddress;

/**
 * Created by brecht on 3/7/15.
 */
public class ArDrone2 extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef senderRef;

    private final ActorRef udpManager;
    private final ActorRef listener;

    private DroneConnectionDetails details;
    private InetSocketAddress senderAddress;

    private int seq = 0;

    public ArDrone2(DroneConnectionDetails details, final ActorRef listener) {
        this.senderAddress = new InetSocketAddress(details.getIp(), details.getSendingPort());
        this.details = details;

        this.listener = listener;

        udpManager = Udp.get(getContext().system()).getManager();
        udpManager.tell(UdpMessage.bind(getSelf(), senderAddress), getSelf());
    }

    @Override
    public void preStart() {
        log.info("Starting ARDrone 2.0 communication to [{}]:[{}]", details.getIp(), details.getSendingPort());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof Udp.Bound) {
            log.debug("Socket ARDRone 2.0 bound.");

            senderRef = getSender();
            getContext().become(ready(senderRef));
        } else if (msg instanceof StopMessage) {
            stop();
        } else {
            unhandled(msg);
        }
    }

    private Procedure<Object> ready(final ActorRef socket) {
        return msg -> {
            if (msg instanceof Udp.Received) {
                final Udp.Received r = (Udp.Received) msg;
                processRawData(r.data());
            } else if (msg.equals(UdpMessage.unbind())) {
                socket.tell(msg, getSelf());
            } else if (msg instanceof Udp.Unbound) {
                getContext().stop(getSelf());
            } else if (msg instanceof DroneCommandMessage) {
                dispatchCommand((DroneCommandMessage) msg);
            } else if (msg instanceof StopMessage) {
                stop();
            } else unhandled(msg);
        };
    }

    private void dispatchCommand(DroneCommandMessage msg) {
        if (msg.getMessage() instanceof TakeOffCommand) {
            handleTakeoff();
        } else if (msg.getMessage() instanceof LandCommand) {
            handleLand();
        } else if (msg.getMessage() instanceof InitDroneCommand) {
            handleInit();
        } else {
            log.warning("No handler for: [{}]", msg.getMessage().getClass().getCanonicalName());
        }
    }

    private void stop() {
        udpManager.tell(UdpMessage.unbind(), self());
        getContext().stop(self());
    }

    public boolean sendData(ByteString data) {
        if (senderAddress != null && senderRef != null) {
            log.debug("Sending RAW data.");
            senderRef.tell(UdpMessage.send(data, senderAddress), getSelf());
            return true;
        } else {
            log.debug("Sending data without discovery data available.");
            return false;
        }
    }

    private void processRawData(ByteString data) {
        /*ByteString current = recvBuffer == null ? data : recvBuffer.concat(data); //immutable rolling buffer

        ByteIterator it = current.iterator();
        while(it.hasNext()) {
            System.out.println("");
        }*/
        // Do something

    }

    private void handle(TakeOffCommand cmd) {
        sendData(PacketCreator.createTakeOffPacket());
    }

    private void handleTakeoff() {
        sendData(PacketCreator.createTakeOffPacket());
    }

    private void handle(LandCommand cmd) {
        sendData(PacketCreator.createLandingPacket());
    }

    private void handleLand() {
        sendData(PacketCreator.createLandingPacket());
    }

    private void handle(EmergencyCommand cmd) {
        sendData(PacketCreator.createEmergencyPacket());
    }

    /**
     * For the init code see: https://github.com/puku0x/cvdrone/blob/master/src/ardrone/command.cpp
     */
    private void handleInit() {
        sendData(PacketCreator.createPacket(new ATCommandPMODE(2))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandMISC(2,20,2000,3000))); // Undocumented command
        sendData(PacketCreator.createPacket(new ATCommandFTRIM()));
        sendData(PacketCreator.createPacket(new ATCommandCOMWDG()));
        sendData(PacketCreator.createPacket(new ATCommandCONFIG("control:altitude_max", "10000"))); // 10m max height
        sendData(PacketCreator.createPacket(new ATCommandCONFIG("general:navdata_demo", "TRUE")));
    }

    public LoggingAdapter getLog(){
        return log;
    }
}
