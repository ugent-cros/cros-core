package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class UnsubscribeEventMessage implements Serializable {
    private Class subscribedClass;

    public UnsubscribeEventMessage(Class subscribedClass) {
        this.subscribedClass = subscribedClass;
    }

    public UnsubscribeEventMessage(){}

    public Class getSubscribedClass() {
        return subscribedClass;
    }
}
