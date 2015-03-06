package drones.models;

import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import drones.util.FrameHelper;

import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Cedric on 3/6/2015.
 */
public class Channel {
    public final static int INFINITE_RETRY = -1;

    private byte id;
    private FrameType type;
    private byte seq;
    private int sendDelay;
    private int ackTimeout;
    private int numRetry;

    private final Object lock = new Object();
    private final ConcurrentLinkedQueue<AckedFrame> frameQueue;

    public Channel(byte id, FrameType type, byte seq) {
        this(id, type, seq, 20, -1, INFINITE_RETRY);
    }

    public Channel(byte id, FrameType type, byte seq, int sendDelay, int ackTimeout, int numRetry) {
        this.id = id;
        this.type = type;
        this.seq = seq;
        this.sendDelay = sendDelay;
        this.ackTimeout = ackTimeout;
        this.numRetry = numRetry;

        this.frameQueue = new ConcurrentLinkedQueue<>();
    }

    public void send(ByteString data){
        ByteString frameLoad = FrameHelper.getFrameData(createFrame(data));

        if(type == FrameType.DATA_WITH_ACK){

        } else {

        }
    }

    private void trySendAcked(){

    }

    private Frame createFrame(ByteString data){
        byte s = 0;
        synchronized (lock){
            s = this.seq++;
        }
        return new Frame(this.type, this.id, s, data);
    }

    public byte getId() {
        return id;
    }

    public FrameType getType() {
        return type;
    }

    public byte getSeq() {
        return seq;
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
