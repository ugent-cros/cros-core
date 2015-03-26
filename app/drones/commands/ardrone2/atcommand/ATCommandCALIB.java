package drones.commands.ardrone2.atcommand;

/**
 * Created by brecht on 3/26/15.
 */
public class ATCommandCALIB extends ATCommand {
    private static final String TYPE = "AT*CALIB";
    private int deviceNumber;

    public ATCommandCALIB(int seq, int deviceNumber) {
        super(seq);
        this.deviceNumber = deviceNumber;
    }

    public ATCommandCALIB(int seq) {
        this(seq, 0);
    }

    @Override
    public String toString() {
        return String.format("%s=%d,%d\r",TYPE, seq, deviceNumber);
    }
}
