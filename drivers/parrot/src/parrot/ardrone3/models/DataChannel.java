package parrot.ardrone3.models;

import akka.util.ByteString;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final int sendDelay;
    private final int ackTimeout;
    private final int numRetry;

    //Concurrency primitives
    private LinkedList<Frame> frameQueue;
    private long lastSend = 0;
    private int retried = 0;

    private AtomicInteger missed;
    private AtomicInteger sent;

    public DataChannel(byte id, FrameType type) {
        this(id, type, 20, -1, INFINITE_RETRY);
    }

    public DataChannel(byte id, FrameType type, int sendDelay, int ackTimeout, int numRetry) {
        this.id = id;
        this.type = type;
        this.seq = 1;

        this.missed = new AtomicInteger(0);
        this.sent = new AtomicInteger(0);

        this.sendDelay = sendDelay;
        this.ackTimeout = ackTimeout;
        this.numRetry = numRetry;

        if (type == FrameType.DATA_WITH_ACK) { //Create a sender buffer if necesary
            if (numRetry == INFINITE_RETRY)
                throw new IllegalArgumentException("numRetry cannot be infinite with ACK channel");
            this.frameQueue = new LinkedList<>();
        }
    }

    public Frame sendFrame(Frame f, long time) {
        sent.incrementAndGet(); //TODO: atomic
        if (type == FrameType.DATA_WITH_ACK) {
            synchronized (lock) {
                frameQueue.offer(f);
            }
            return tick(time);
        } else return f;
    }

    public Frame tick(long time) {
        synchronized (lock) {
            if (!frameQueue.isEmpty()) {
                long diff = time - lastSend;
                if (diff > ackTimeout) {
                    if (retried >= numRetry) {
                        missed.incrementAndGet();//TODO: atomic
                        retried = 0;
                        frameQueue.poll(); // pop frame
                        if (frameQueue.isEmpty()) {
                            lastSend = 0; //make ready for next packet
                            return null;
                        }
                    }
                    lastSend = time;
                    retried++;
                    return frameQueue.peek();
                }
            }
        }
        return null;
    }

    public boolean shouldAllowFrame(Frame f) {
        // Warning: signed java bytes vs. unsigned seq
        // TODO: Use atomic inc / cmpexch
        synchronized (lock) {
            seq = f.getSeq();
        }
        return true; //TODO: implement properly and don't accept all packets
      /*
        byte s = 0;
        synchronized (lock){
            s = seq;
        }


        byte retVal = (byte)(f.getSeq() - s);
        if (retVal < MAX_ALLOWED_SEQ_OFFSET)// If the packet is more than 10 seq old, it might be a loopback
        {
            retVal += MAX_ALLOWED_SEQ_OFFSET;
        }
        return retVal != 0;*/
    }

    public Frame receivedAck(byte seq, long time) {
        synchronized (lock) {
            if(!frameQueue.isEmpty()) {
                Frame f = frameQueue.peek();
                if (f.getSeq() == seq) {
                    frameQueue.poll();
                    lastSend = 0;
                    retried = 0;
                }
            }
        }
        return tick(time);
    }

    public Frame createFrame(ByteString data) {
        byte s = 0;
        synchronized (lock) {
            s = this.seq++;
        }
        return new Frame(this.type, this.id, s, data);
    }

    public float getMissRate(){
        return (float)missed.get() / sent.get();
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
