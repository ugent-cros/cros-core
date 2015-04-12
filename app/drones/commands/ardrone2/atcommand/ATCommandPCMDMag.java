package drones.commands.ardrone2.atcommand;

/**
 * The command looks like: AT*PCMD_MAG=<SEQ>,<FLAG>,<ROLL>,<PITCH>,<GAZ>,<YAW>,<PSI>,<ACCPSI>\r
 *
 * Created by brecht on 3/26/15.
 */
public class ATCommandPCMDMag extends ATCommandPCMD {
    private float psi;
    private float accPsi;

    // The command name
    private static final String COMMAND_NAME = "PCMD_MAG";

    /**
     *
     * @param seq The sequence number of the command
     * @param flag
     * @param roll
     * @param pitch
     * @param gaz
     * @param yaw
     * @param psi
     * @param accPsi
     *
     * Other parameters: see A.R.Drone Developer Guide (p. 36)
     */
    public ATCommandPCMDMag(int seq, int flag, float roll, float pitch, float gaz, float yaw, float psi, float accPsi) {
        super(seq, flag, roll, pitch, gaz, yaw);

        super.setCommandName(COMMAND_NAME);

        this.psi = psi;
        this.accPsi = accPsi;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d,%d,%d,%d,%d,%d,%d", seq, flag,
                intOfFloat(roll), intOfFloat(pitch), intOfFloat(gaz), intOfFloat(yaw), intOfFloat(psi), intOfFloat(accPsi));
    }

}
