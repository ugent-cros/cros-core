package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/24/15.
 */
public class ATCommandCONTROL extends ATCommand {
    private int param1;
    private int param2;

    public ATCommandCONTROL(int seq) {
        this(seq, 5, 0);
    }

    public ATCommandCONTROL(int seq, int param1, int param2) {
        super(seq);

        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public String toString() {
        return String.format("AT*CTRL=%d,%d,%d\r", seq, param1, param2);
    }
}
