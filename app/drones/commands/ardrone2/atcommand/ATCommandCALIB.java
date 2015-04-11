package drones.commands.ardrone2.atcommand;

/**
 * This command asks the drone to calibrate the magneto meter (The drone must be flying)
 * The command looks like: AT*CALIB=<SEQ>,<DEVICE_ID>\r
 *
 * Created by brecht on 3/26/15.
 */
public class ATCommandCALIB extends ATCommand {
    // The device ID
    private int deviceNumber;

    // The command name
    private static final String COMMAND_NAME = "CALIB";

    /**
     *
     * @param seq The sequence number of the command
     * @param deviceNumber The device ID that must be calibrated
     */
    public ATCommandCALIB(int seq, int deviceNumber) {
        super(seq, COMMAND_NAME);
        this.deviceNumber = deviceNumber;
    }

    /**
     * The magneto meter will be calibrated
     *
     * @param seq The sequence number of the command
     */
    public ATCommandCALIB(int seq) {
        this(seq, 0);
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d", seq, deviceNumber);
    }
}
