package drones.models.scheduler.messages.to;

/**
 * Created by Ronald on 13/04/2015.
 */
public class SubscribeMessage {

    private Class eventType;

    public SubscribeMessage(Class eventType) {
        this.eventType = eventType;
    }

    public Class getEventType() {
        return eventType;
    }
}
