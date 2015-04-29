package drones.ardrone2.commands;

/**
 * The command looks like: AT*CTRL=<SEQ>,<PARAM1>,<PARAM2>\r
 *
 * Created by brecht on 3/24/15.
 */
public class ATCommandCONTROL extends ATCommand {
    // The command name
    private static final String COMMAND_NAME = "CTRL";

    // Undocumented parameters
    private int param1;
    private int param2;

    /**
     *
     * @param seq The sequence number of the command
     */
    public ATCommandCONTROL(int seq) {
        this(seq, 5, 0);
    }

    /**
     *
     * @param seq The sequence number of the command
     * @param param1 Undocumented parameter
     * @param param2 Undocumented parameter
     */
    public ATCommandCONTROL(int seq, int param1, int param2) {
        super(seq, COMMAND_NAME);

        this.param1 = param1;
        this.param2 = param2;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d,%d", seq, param1, param2);
    }

}
