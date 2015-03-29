package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandPCMD extends ATCommand {
    // Params command REF
    protected int flag;
    protected float roll;
    protected float pitch;
    protected float gaz;
    protected float yaw;

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
        return String.format("AT*PCMD=%d,%d,%d,%d,%d,%d\r", seq, flag, intOfFloat(roll), intOfFloat(pitch), intOfFloat(gaz), intOfFloat(yaw));
    }
}
