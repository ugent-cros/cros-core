package drones.models.ATCommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandPCMD extends ATCommand {
    private static final String TYPE = "PCMD";

    // Params command REF
    private int flag;
    private int roll;
    private int pitch;
    private int gaz;
    private int yaw;

    public ATCommandPCMD(int ID, int flag, int roll, int pitch, int gaz, int yaw) {
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
