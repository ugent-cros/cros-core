package messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class ExecutionResultMessage implements Serializable {
    private Object value;

    public ExecutionResultMessage(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
