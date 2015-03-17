package drones.commands.ardrone2.atcommand;

/**
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandPMODE extends ATCommand {
    private static final String TYPE = "AT*PMODE";

    private int param1;

    public ATCommandPMODE(int seq, int param1) {
        super(seq);
        this.param1 = param1;
    }

    @Override
    public String toString() {
        return String.format("%s=%d,%d\r",TYPE, seq, param1);
    }
}
