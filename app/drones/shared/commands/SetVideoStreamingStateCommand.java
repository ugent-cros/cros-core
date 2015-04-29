package drones.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SetVideoStreamingStateCommand implements Serializable {
    private boolean enabled;

    public SetVideoStreamingStateCommand(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
