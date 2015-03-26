package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/12/15.
 */
public class ATCommandFTRIM extends ATCommand {
    public ATCommandFTRIM(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return String.format("AT*FTRIM=%d\r",TYPE, seq);
    }
}
