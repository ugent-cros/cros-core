package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class ExecutionResultMessage<T> implements Serializable {
    T value;

    public ExecutionResultMessage(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
