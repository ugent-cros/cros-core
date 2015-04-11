package drones.commands.ardrone2.atcommand;

/**
 * This command resets communication watchdog
 * The command looks like: AT*COMWDG=<SEQ>\r
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandCOMWDG extends ATCommand {
    // The command name
    private static final String COMMAND_NAME = "COMWDG";

    /**
     *
     * @param seq The sequence number of the command
     */
    public ATCommandCOMWDG(int seq) {
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
