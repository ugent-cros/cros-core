package drones.ardrone2.commands;

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
        super(seq, COMMAND_NAME);

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

}
