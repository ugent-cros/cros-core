package drones.models.scheduler.messages.to;

/**
 * Created by Ronald on 13/04/2015.
 */
public class UnsubscribeMessage {

    private Class eventType;

    public UnsubscribeMessage(Class messageType) {
        this.eventType = messageType;
    }

    public Class getEventType() {
        return eventType;
    }
}
