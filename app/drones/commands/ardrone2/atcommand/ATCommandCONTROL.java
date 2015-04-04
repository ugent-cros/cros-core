package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/24/15.
 */
public class ATCommandCONTROL extends ATCommand {
    public ATCommandCONTROL(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return String.format("AT*CTRL=%d,%d,%d\r", seq, 5, 0);
    }
}
