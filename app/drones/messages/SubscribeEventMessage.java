package drones.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SubscribeEventMessage implements Serializable {
    private Class subscribedClass;

    public SubscribeEventMessage(Class subscribedClass) {
        this.subscribedClass = subscribedClass;
    }

    public Class getSubscribedClass() {
        return subscribedClass;
    }
}
