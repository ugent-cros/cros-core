package drones.scheduler.messages.from;

/**
 * Created by Ronald on 18/04/2015.
 */
public class SchedulerReplyMessage implements SchedulerEvent{

    private long requestId;

    public SchedulerReplyMessage(long requestId) {
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }
}
