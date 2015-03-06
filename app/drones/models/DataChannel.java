package drones.models;

import akka.util.ByteString;
import drones.protocols.ArDrone3;
import drones.util.FrameHelper;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Cedric on 3/6/2015.
 */
public class DataChannel extends Channel {
    public final static int INFINITE_RETRY = -1;

    private final ArDrone3 comChannel;

    // Ack channel specs
    private int sendDelay;
    private int ackTimeout;
    private int numRetry;

    //Concurrency primitives
    private ConcurrentLinkedQueue<AckedFrame> frameQueue;

    public DataChannel(ArDrone3 comChannel, byte id, FrameType type) {
        this(comChannel, id, type, 20, -1, INFINITE_RETRY);
    }

    public DataChannel(ArDrone3 comChannel, byte id, FrameType type, int sendDelay, int ackTimeout, int numRetry) {
        super(type, id);

        this.comChannel = comChannel;

        this.sendDelay = sendDelay;
        this.ackTimeout = ackTimeout;
        this.numRetry = numRetry;

        if(type == FrameType.DATA_WITH_ACK) { //Create a sender buffer if necesary
            this.frameQueue = new ConcurrentLinkedQueue<>();
            if(numRetry == INFINITE_RETRY)
                throw new IllegalArgumentException("numRetry cannot be infinite with ACK channel");
        }
    }

    public void sendFrame(Frame frame){
        ByteString frameLoad = FrameHelper.getFrameData(frame);
        if(type == FrameType.DATA_WITH_ACK){
            //TODO enqueue packets, monitor timeouts
        } else {
            comChannel.getLog().debug("Sent non-acked packet on ID=[{}], TYPE=[{}], SEQ=[{}]", frame.getId(), frame, frame.getSeq());
        }
    }

    public void sendWithFrame(ByteString data){
        sendFrame(createFrame(data));
    }

    public int getSendDelay() {
        return sendDelay;
    }

    public int getAckTimeout() {
        return ackTimeout;
    }

    public int getNumRetry() {
        return numRetry;
    }
}
