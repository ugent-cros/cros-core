package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/24/15.
 */
public class ATCommandCONTROL extends ATCommand {
    private static final String TYPE = "AT*CONTROL";

    public ATCommandCONTROL() {
        super(-1);
    }

    @Override
    public String toString() {
        return String.format("%s=%d\r",TYPE, 0);
    }
}
