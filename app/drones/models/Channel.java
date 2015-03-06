package drones.models;

import akka.util.ByteString;

/**
 * Created by Cedric on 3/6/2015.
 */
public abstract class Channel {
    // Channel specs
    protected final byte id;
    protected final FrameType type;
    protected byte seq;

    private final Object lock = new Object();

    public Channel(FrameType type, byte id){
        this.id = id;
        this.type = type;
        this.seq = 0;
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
}
