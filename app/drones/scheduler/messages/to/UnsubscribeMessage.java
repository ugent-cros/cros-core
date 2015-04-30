package drones.scheduler.messages.to;

import drones.scheduler.messages.from.SchedulerEvent;

/**
 * Created by Ronald on 13/04/2015.
 */
public class UnsubscribeMessage {

    private Class<? extends SchedulerEvent> eventType;

    public UnsubscribeMessage(Class<? extends SchedulerEvent> messageType) {
        this.eventType = messageType;
    }

    public Class<? extends SchedulerEvent> getEventType() {
        return eventType;
    }
}
