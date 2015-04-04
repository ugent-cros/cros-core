package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/31/2015.
 */
public class ConnectionStatusChangedMessage implements Serializable {
    private boolean connected;

    public ConnectionStatusChangedMessage(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
