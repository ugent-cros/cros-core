package parrot.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 5/6/2015.
 */
public class SetControllerStateCommand implements Serializable {
    private boolean enabled;

    public SetControllerStateCommand(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
