package drones.scheduler.messages.to;

import drones.scheduler.messages.from.SchedulerEvent;

/**
 * Created by Ronald on 20/04/2015.
 */
public class SchedulerPublishMessage {

    private SchedulerEvent event;

    public SchedulerPublishMessage(SchedulerEvent event) {
        this.event = event;
    }

    public SchedulerEvent getEvent() {
        return event;
    }
}
