package parrot.ardrone3.models;

import akka.util.ByteString;

/**
 * Created by Cedric on 3/6/2015.
 */
public class Frame {

    private FrameType type;
    private byte id;
    private byte seq;

    private ByteString data;

    public Frame(FrameType type, byte id, byte seq, ByteString data) {
        this.type = type;
        this.id = id;
        this.seq = seq;
        this.data = data;
    }

    public FrameType getType() {
        return type;
    }

    public byte getId() {
        return id;
    }

    public byte getSeq() {
        return seq;
    }

    public ByteString getData() {
        return data;
    }
}
