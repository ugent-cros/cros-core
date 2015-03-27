package drones.simulation.messages;

import java.io.Serializable;

/**
 * Created by yasser on 26/03/15.
 */
public class SetCrashedMessage implements Serializable {

    private boolean crashed;

    public SetCrashedMessage(boolean crashed) {
        this.crashed = crashed;
    }

    public boolean isCrashed() {
        return crashed;
    }
}
