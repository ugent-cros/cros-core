package drones.scheduler.messages.to;

/**
 * Created by Ronald on 18/04/2015.
 */
public class SchedulerRequestMessage {

    private static long count = 0;
    private final long requestId;

    public SchedulerRequestMessage() {
        requestId = count;
        count++;
    }

    public long getRequestId() {
        return requestId;
    }
}
