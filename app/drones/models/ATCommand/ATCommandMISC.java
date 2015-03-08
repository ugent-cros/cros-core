package drones.models.ATCommand;

/**
 * @TODO see what this command does
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandMISC extends ATCommand {
    private static final String TYPE = "AT*MISC";

    private int param1, param2, param3, param4;

    public ATCommandMISC(int param1, int param2, int param3, int param4) {
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }

    @Override
    public String toString() {
        String params = param1 + "," + param2 + "," + param3 + "," + param4;
        return TYPE + "=" + seq + "," + params + "\r";
    }
}
