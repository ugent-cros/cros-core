package messages;

import model.properties.AlertState;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class AlertStateChangedMessage implements Serializable {

    private AlertState state;

    public AlertStateChangedMessage(AlertState state) {
        this.state = state;
    }

    public AlertState getState() {
        return state;
    }
}
