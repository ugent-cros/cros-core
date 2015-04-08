package drones.commands.ardrone2.atcommand;

/**
 * The command looks like: AT*REF=<SEQ>,<BIT VECTOR>\r
 *
 * Created by brecht on 3/7/15.
 */
public class ATCommandREF extends ATCommand {
    // Bit vector
    private int input;

    // The command name
    private static final String COMMAND_NAME = "REF";


    /**
     *
     * @param seq The sequence number of the command
     * @param input A bit vector
     */
    public ATCommandREF(int seq, int input) {
        super(seq);

        this.input = input;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,%d", seq, input);
    }

    /**
     *
     * @return The name of the command
     */
    @Override
    protected String getCommandName() {
        return COMMAND_NAME;
    }
}
