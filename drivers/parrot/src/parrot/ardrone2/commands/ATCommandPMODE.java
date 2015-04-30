package parrot.ardrone2.commands;

/**
 * The command looks like: AT*PMODE=<SEQ>,<PARAM1>\r
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandPMODE extends ATCommand {
    private int param1;

    // The command name
    private static final String COMMAND_NAME = "PMODE";

    /**
     *
     * @param seq The sequence number of the command
     * @param param1 Undocumented parameter
     */
    public ATCommandPMODE(int seq, int param1) {
        super(seq, COMMAND_NAME);

        this.param1 = param1;
    }

    /**
     *
     * @param seq The sequence number of the command
     */
    public ATCommandPMODE(int seq) {
        this(seq, 2);
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d", seq, param1);
    }

}
