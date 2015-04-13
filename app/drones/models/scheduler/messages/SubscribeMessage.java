package drones.models.scheduler.messages;

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
