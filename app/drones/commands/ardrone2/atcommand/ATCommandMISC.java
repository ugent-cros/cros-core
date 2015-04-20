package drones.commands.ardrone2.atcommand;

/**
 * The command looks like: AT*MISC=<SEQ>,<PARAM1>,<PARAM2>,<PARAM3>,<PARAM4>\r
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandMISC extends ATCommand {
    private int param1, param2, param3, param4;

    // The command name
    private static final String COMMAND_NAME = "MISC";

    /**
     *
     * @param seq The sequence number of the command
     * @param param1 Undocumented parameter
     * @param param2 Undocumented parameter
     * @param param3 Undocumented parameter
     * @param param4 Undocumented parameter
     */
    public ATCommandMISC(int seq, int param1, int param2, int param3, int param4) {
        super(seq, COMMAND_NAME);
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }

    /**
     *
     * @param seq The sequence number of the command
     */
    public ATCommandMISC(int seq) {
        this(seq, 2, 20, 2000, 3000);
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d,%d,%d,%d", seq, param1, param2, param3, param4);
    }

}
