package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/26/15.
 */
public class ATCommandPCMDMag extends ATCommandPCMD {
    private int psi;
    private int accPsi;

    public ATCommandPCMDMag(int seq, int flag, float roll, float pitch, float gaz, float yaw, float psi, float accPsi) {
        super(seq, flag, roll, pitch, gaz, yaw);
    }

    @Override
    public String toString() {
        return String.format("AT*PCMD_MAG=%d,%d,%d,%d,%d,%d,%d,%d\r", seq, flag,
                intOfFloat(roll), intOfFloat(pitch), intOfFloat(gaz), intOfFloat(yaw),
                intOfFloat(psi), intOfFloat(accPsi));
    }
}
