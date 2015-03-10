package drones.messages;

import drones.models.FlyingState;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class FlyingStateChangedMessage implements Serializable {

    private FlyingState state;

    public FlyingStateChangedMessage(FlyingState state) {
        this.state = state;
    }

    public FlyingState getState() {
        return state;
    }


}
