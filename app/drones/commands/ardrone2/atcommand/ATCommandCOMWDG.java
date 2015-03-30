package drones.commands.ardrone2.atcommand;

/**
 *
 * Created by brecht on 3/8/15.
 */
public class ATCommandCOMWDG extends ATCommand {
    public ATCommandCOMWDG(int seq) {
        super(seq);
    }

    @Override
    public String toString() {
        return String.format("AT*COMWDG=%d\r", seq);
    }
}
