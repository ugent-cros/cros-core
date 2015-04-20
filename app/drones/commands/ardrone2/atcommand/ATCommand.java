package drones.commands.ardrone2.atcommand;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by brecht on 3/7/15.
 *
 * Implementation of a ARDrone 2.0 command
 */
public abstract class ATCommand {
    private String commandName;
    protected int seq;
    private FloatBuffer fb;
    private IntBuffer ib;

    /**
     *
     * @param seq The sequence number of the command
     */
    public ATCommand(int seq, String commandName) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        fb = bb.asFloatBuffer();
        ib = bb.asIntBuffer();

        this.seq = seq;
        this.commandName = commandName;
    }

    /**
     *
     * @param seq The sequence number of the command
     */
    public void setSeq(int seq) {
        this.seq = seq;
    }

    /**
     *
     * @return The sequence number of the command
     */
    public int getSeq() {
        return seq;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    protected abstract String parametersToString();

    /**
     *
     * @return The name of the command
     */
    private String getCommandName() {
        return commandName;
    }

    /**
     *
     * @param commandName The name of the command
     */
    protected void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    /**
     *
     * @return The command as a string (For further explanation see the A.R.Drone Developer Guide, p. 30)
     */
    @Override
    public String toString() {
        return String.format("AT*%s=%s\r", getCommandName(), parametersToString());
    }

    /**
     * This method converts a float to an int (For further explanation see the A.R.Drone Developer Guide, p. 31)
     *
     * @param f The float that needs to be converted
     * @return The converted float
     */
    protected int intOfFloat(float f) {
        fb.put(0, f);
        return ib.get(0);
    }
}
