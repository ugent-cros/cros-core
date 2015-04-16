package drones.models.scheduler.messages.to;

/**
 * Created by Ronald on 13/04/2015.
 */
public class UnsubscribeMessage {

    private Class messageType;

    public UnsubscribeMessage(Class messageType) {
        this.messageType = messageType;
    }

    public Class getMessageType() {
        return messageType;
    }
}
