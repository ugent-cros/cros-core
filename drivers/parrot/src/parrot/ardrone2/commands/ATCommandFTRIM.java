package parrot.ardrone2.commands;

/**
 * The command looks like: AT*FTRIM=<SEQ>\r
 *
 * Created by brecht on 3/12/15.
 */
public class ATCommandFTRIM extends ATCommand {
    // The command name
    private static final String COMMAND_NAME = "FTRIM";

    /**
     *
     * @param seq The sequence number of the command
     */
    public ATCommandFTRIM(int seq) {
        super(seq, COMMAND_NAME);
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d", seq);
    }


}
