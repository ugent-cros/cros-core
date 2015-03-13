package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandPCMD extends ATCommand {
    private static final String TYPE = "AT*PCMD";

    // Params command REF
    private int flag;
    private float roll;
    private float pitch;
    private float gaz;
    private float yaw;

    public ATCommandPCMD(int seq, int flag, float roll, float pitch, float gaz, float yaw) {
        super(seq);
        this.flag = flag;
        this.roll = roll;
        this.pitch = pitch;
        this.gaz = gaz;
        this.yaw = yaw;
    }

    /**
     *
     * @return PCMD command, e.g.: "AT*PCMD=<SEQ>,<FLAG>,<ROLL><PITCH>,<GAZ>,<YAW>\r"
     */
    @Override
    public String toString() {
        return String.format("%s=%d,%d,%d,%d,%d,%d\r", TYPE, seq, flag, intOfFloat(roll), intOfFloat(pitch), intOfFloat(gaz), intOfFloat(yaw));
    }
}
