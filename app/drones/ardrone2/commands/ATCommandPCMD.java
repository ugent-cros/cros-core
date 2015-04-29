package drones.ardrone2.commands;

/**
 * The command looks like: AT*PCMD=<SEQ>,<FLAG>,<ROLL>,<PITCH>,<GAZ>,<YAW>\r
 *
 * Created by brecht on 3/7/15.
 */
public class ATCommandPCMD extends ATCommand {
    // Params command PCMD
    protected int flag;
    protected float roll;
    protected float pitch;
    protected float gaz;
    protected float yaw;

    // The command name
    private static final String COMMAND_NAME = "PCMD";

    /**
     *
     * @param seq
     * @param flag
     * @param roll
     * @param pitch
     * @param gaz
     * @param yaw
     *
     * Parameters: see A.R.Drone Developer Guide (p. 36)
     */
    public ATCommandPCMD(int seq, int flag, float roll, float pitch, float gaz, float yaw) {
        super(seq, COMMAND_NAME);

        this.flag = flag;
        this.roll = roll;
        this.pitch = pitch;
        this.gaz = gaz;
        this.yaw = yaw;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d,%d,%d,%d,%d", seq, flag,
                intOfFloat(roll), intOfFloat(pitch), intOfFloat(gaz), intOfFloat(yaw));
    }

}
