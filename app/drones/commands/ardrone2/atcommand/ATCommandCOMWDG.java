package drones.commands.ardrone2.atcommand;

/**
 * @TODO see what this command does
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandCOMWDG extends ATCommand {
    private static final String TYPE = "AT*COMWDG";

    public ATCommandCOMWDG(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return String.format("%s=%d\r",TYPE, seq);
    }
}
