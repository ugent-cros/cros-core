package drones.models.scheduler.messages.to;

import drones.models.scheduler.messages.from.SchedulerEvent;

/**
 * Created by Ronald on 13/04/2015.
 */
public class SubscribeMessage {

    private Class<? extends SchedulerEvent> eventType;

    public SubscribeMessage(Class<? extends SchedulerEvent> eventType) {
        this.eventType = eventType;
    }

    public Class<? extends SchedulerEvent> getEventType() {
        return eventType;
    }
}
