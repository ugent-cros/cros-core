package drones.models;

import akka.util.ByteString;

/**
 * Created by Cedric on 3/6/2015.
 */
public class AckedFrame  {
    private ByteString data;
    private long sent;
    private int retries;

    public AckedFrame(ByteString frame, long sent, int retries){
        this.data = frame;
        this.sent = sent;
        this.retries = retries;
    }

    public ByteString getFrame() {
        return data;
    }

    public long getSent() {
        return sent;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void setSent(long sent) {
        this.sent = sent;
    }
}
