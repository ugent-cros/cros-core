package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandREF extends ATCommand {
    private static final String TYPE = "AT*REF";

    // Params command REF
    private int input;

    public ATCommandREF(float input) {
        this.input = intOfFloat(input);
    }

    public ATCommandREF(int input) {
        this.input = input;
    }

    /**
     *
     * @return REF command, e.g.: "AT*REF=<SEQ>,<BIT_SEQUENCE>\r"
     *
     */
    @Override
    public String toString() {
        return String.format("%s=%d,%d\r",TYPE, seq, input);
    }
}
