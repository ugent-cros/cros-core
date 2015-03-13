package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/12/15.
 */
public class ATCommandFTRIM extends ATCommand {
    private static final String TYPE = "AT*FTRIM";

    public ATCommandFTRIM(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return String.format("%s=%d\r",TYPE, seq);
    }
}
