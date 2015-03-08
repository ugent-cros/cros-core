package drones.models.ATCommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandPCMD extends ATCommand {
    private static final String TYPE = "PCMD";

    // Params command REF
    private float flag;
    private float roll;
    private float pitch;
    private float gaz;
    private float yaw;

    public ATCommandPCMD(float flag, float roll, float pitch, float gaz, float yaw) {
        this.flag = flag;
        this.roll = roll;
        this.pitch = pitch;
        this.gaz = gaz;
        this.yaw = yaw;
    }

    @Override
    public String toString() {
        String prefix = "AT*" + TYPE + "=";
        String id = Integer.toString(5);
        String params = flag + "," + roll + "," + pitch + "," + gaz + "," + yaw;

        return (prefix + id + "," + params + "\r");
    }
}
