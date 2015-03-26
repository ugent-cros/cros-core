package simulation.messages;

import java.io.Serializable;

/**
 * Created by yasser on 26/03/15.
 */
public class SetConnectionLostMessage implements Serializable {

    private boolean connectionLost;

    public SetConnectionLostMessage(boolean connectionLost) {
        this.connectionLost = connectionLost;
    }

    public boolean isConnectionLost() {
        return connectionLost;
    }
}
