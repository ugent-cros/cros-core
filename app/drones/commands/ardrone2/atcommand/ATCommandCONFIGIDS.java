package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/22/15.
 */
public class ATCommandCONFIGIDS extends ATCommand {
    private static final String TYPE = "AT*CONFIG_IDS";

    private String session;
    private String user;
    private String appIDS;

    public ATCommandCONFIGIDS(int seq, String session, String user, String appIDS) {
        super(seq);

        this.session = session;
        this.user = user;
        this.appIDS = appIDS;
    }

    @Override
    public String toString() {
        return String.format("%s=%d,%s,%s,%s\r", TYPE, seq, session, user, appIDS);
    }
}
