package drones.models.ATCommand;

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

    public ATCommandPCMD(int flag, float roll, float pitch, float gaz, float yaw) {
        this.flag = flag;
        this.roll = roll;
        this.pitch = pitch;
        this.gaz = gaz;
        this.yaw = yaw;
    }

    @Override
    public String toString() {
        String params = flag + "," + intOfFloat(roll) + "," + intOfFloat(pitch) + "," + intOfFloat(gaz) + "," + intOfFloat(yaw);
        return (TYPE + "=" + seq + "," + params + "\r");
    }
}
