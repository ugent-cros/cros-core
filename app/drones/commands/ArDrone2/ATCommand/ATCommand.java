package drones.commands.ArDrone2.ATCommand;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by brecht on 3/7/15.
 *
 * Implementation of a ARDrone 2.0 command
 */
public abstract class ATCommand {
    protected int seq;

    public ATCommand() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        fb = bb.asFloatBuffer();
        ib = bb.asIntBuffer();
    }

    public ATCommand(int seq) {
        this.seq = seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getSeq() {
        return seq;
    }

    private FloatBuffer fb;
    private IntBuffer ib;

    protected int intOfFloat(float f) {
        fb.put(0, f);
        return ib.get(0);
    }
}
