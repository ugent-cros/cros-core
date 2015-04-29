package drones.models.scheduler.messages.from;

import java.io.Serializable;

/**
 * Created by Ronald on 23/04/2015.
 */
public class SubscribedMessage implements Serializable{

    private Class<? extends SchedulerEvent> eventType;

    public SubscribedMessage(Class<? extends SchedulerEvent> event) {
        this.eventType = event;
    }

    public Class<? extends SchedulerEvent> getEventType() {
        return eventType;
    }
}
