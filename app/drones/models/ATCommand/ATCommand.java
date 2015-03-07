package drones.models.ATCommand;

import akka.util.ByteString;

/**
 * Created by brecht on 3/7/15.
 *
 * Implementation of a ARDrone 2.0 command
 */
public abstract class ATCommand {
    protected int seq;

    public ATCommand() {

    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getSeq() {
        return seq;
    }

}
