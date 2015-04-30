package droneapi.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/17/2015.
 */
public class SubscribeEventMessage implements Serializable {
    private Class[] subscriptions;

    public SubscribeEventMessage(Class[] subscribedClass) {
        this.subscriptions = subscribedClass;
    }

    public SubscribeEventMessage(){}

    public Class[] getSubscribedClasses() {
        return subscriptions;
    }
}
