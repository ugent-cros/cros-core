package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class GPSFixChangedMessage implements Serializable{
    boolean fixed;

    public GPSFixChangedMessage(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean isFixed() {
        return fixed;
    }
}
