package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandREF extends ATCommand {
    // Params command REF
    private int input;

    public ATCommandREF(int seq, float input) {
        super(seq);
        this.input = intOfFloat(input);
    }

    public ATCommandREF(int seq, int input) {
        super(seq);
        this.input = input;
    }

    /**
     *
     * @return REF command, e.g.: "AT*REF=<SEQ>,<BIT_SEQUENCE>\r"
     *
     */
    @Override
    public String toString() {
        return String.format("AT*REF=%d,%d\r",TYPE, seq, input);
    }
}
