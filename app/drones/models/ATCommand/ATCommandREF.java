package drones.models.ATCommand;

/**
 * Created by brecht on 3/7/15.
 */
public class ATCommandREF extends ATCommand {
    private static final String TYPE = "REF";

    // Params command REF
    private int input;

    public ATCommandREF(int input) {
        this.input = input;
    }

    @Override
    public String toString() {
        String prefix = "AT*" + TYPE + "=";
        String id = Integer.toString(5);
        String params = Integer.toString(input);

        return (prefix + id + "," + params + "\r");
    }
}
