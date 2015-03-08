package drones.models.ardrone3;

import akka.util.ByteString;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Cedric on 3/6/2015.
 */
public class DataChannel {
    public final static int INFINITE_RETRY = -1;
    public final static byte MAX_ALLOWED_SEQ_OFFSET = 10;

    protected final byte id;
    protected final FrameType type;
    protected byte seq;

    private final Object lock = new Object();

    // Ack channel specs
    private int sendDelay;
    private int ackTimeout;
    private int numRetry;

    //Concurrency primitives
    private ConcurrentLinkedQueue<AckedFrame> frameQueue;

    public DataChannel(byte id, FrameType type) {
        this(id, type, 20, -1, INFINITE_RETRY);
    }

    public DataChannel(byte id, FrameType type, int sendDelay, int ackTimeout, int numRetry) {
        this.id = id;
        this.type = type;
        this.seq = 1;

        this.sendDelay = sendDelay;
        this.ackTimeout = ackTimeout;
        this.numRetry = numRetry;

        if (type == FrameType.DATA_WITH_ACK) { //Create a sender buffer if necesary
            this.frameQueue = new ConcurrentLinkedQueue<>();
            if (numRetry == INFINITE_RETRY)
                throw new IllegalArgumentException("numRetry cannot be infinite with ACK channel");
        }
    }

    public boolean shouldAllowFrame(Frame f){
        byte s = 0;
        synchronized (lock){
            s = seq;
        }

        if(f.getSeq() + MAX_ALLOWED_SEQ_OFFSET < f.getSeq()){ //overflow
            return (f.getSeq() + MAX_ALLOWED_SEQ_OFFSET) + (255 - f.getSeq()) < MAX_ALLOWED_SEQ_OFFSET;
        } else {
            return s - f.getSeq() < MAX_ALLOWED_SEQ_OFFSET;
        }
    }

    public void receivedAck(byte seq){

    }

    public Frame createFrame(ByteString data){
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
