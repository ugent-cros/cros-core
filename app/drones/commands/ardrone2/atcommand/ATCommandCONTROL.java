package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/24/15.
 */
public class ATCommandCONTROL extends ATCommand {
    private static final String TYPE = "AT*CTRL";

    public ATCommandCONTROL(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return String.format("%s=%d,%d,%d\r",TYPE, seq, 5, 0);
    }
}
