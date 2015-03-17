package drones.commands.ardrone2.atcommand;

/**
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandMISC extends ATCommand {
    private static final String TYPE = "AT*MISC";

    private int param1, param2, param3, param4;

    public ATCommandMISC(int seq, int param1, int param2, int param3, int param4) {
        super(seq);
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }

    @Override
    public String toString() {
        return String.format("%s=%d,%d,%d,%d,%d\r",TYPE, seq, param1, param2, param3, param4);
    }
}
