package parrot.ardrone2.commands;

/**
 * The command looks like: AT*CONFIG_IDS=<SEQ>,"<SESSION_ID>","<PROFILE_ID>","<APPLICATION_ID>"\r
 *
 * Created by brecht on 3/22/15.
 */
public class ATCommandCONFIGIDS extends ATCommand {
    // The config IDs
    private String session;
    private String user;
    private String appIDS;

    // Default session IDs
    private static final String ARDRONE_SESSION_ID     = "d2e081a3";  // SessionID
    private static final String ARDRONE_PROFILE_ID     = "be27e2e4";  // Profile ID
    private static final String ARDRONE_APPLICATION_ID = "d87f7e0c";  // Application ID

    // The command name
    private static final String COMMAND_NAME = "CONFIG_IDS";

    /**
     * This command will send the default identification
     *
     * @param seq The sequence number of the command
     */
    public ATCommandCONFIGIDS(int seq) {
        this(seq, ARDRONE_SESSION_ID, ARDRONE_PROFILE_ID, ARDRONE_APPLICATION_ID);
    }

    /**
     *
     * @param seq The sequence number of the command
     * @param session The session ID
     * @param user The user ID
     * @param appIDS The application ID
     */
    public ATCommandCONFIGIDS(int seq, String session, String user, String appIDS) {
        super(seq, COMMAND_NAME);

        this.session = session;
        this.user = user;
        this.appIDS = appIDS;
    }

    /**
     *
     * @return The parameters returned as a string. They are separated by a ",".
     */
    @Override
    protected String parametersToString() {
        return String.format("%d,\"%s\",\"%s\",\"%s\"", seq, session, user, appIDS);
    }

}
