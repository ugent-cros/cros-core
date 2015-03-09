package drones.models.ATCommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandREF extends ATCommand {
    private static final String TYPE = "AT*REF";

    // Params command REF
    private float input;

    public ATCommandREF(float input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return String.format("%s=%d,%d\r",TYPE, seq, intOfFloat(input));
    }
}
