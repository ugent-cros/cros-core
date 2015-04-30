package droneapi.messages;

import droneapi.model.properties.NavigationState;
import droneapi.model.properties.NavigationStateReason;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class NavigationStateChangedMessage implements Serializable {
    private NavigationState state;
    private NavigationStateReason reason;

    public NavigationStateChangedMessage(NavigationState state, NavigationStateReason reason) {
        this.state = state;
        this.reason = reason;
    }

    public NavigationState getState() {
        return state;
    }

    public NavigationStateReason getReason() {
        return reason;
    }
}
