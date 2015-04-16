package drones.models.scheduler.messages.to;

/**
 * Created by Ronald on 13/04/2015.
 */
public class SubscribeMessage {

    private Class messageType;

    public SubscribeMessage(Class messageType) {
        this.messageType = messageType;
    }

    public Class getMessageType() {
        return messageType;
    }
}
