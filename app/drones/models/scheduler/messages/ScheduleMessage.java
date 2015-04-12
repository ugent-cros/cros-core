package drones.models.scheduler.messages;

import java.io.Serializable;

/**
 * Created by Ronald on 12/04/2015.
 */
public class ScheduleMessage implements Serializable {

    private static long count = 0;
    private final long messageId;

    public ScheduleMessage() {
        messageId = count;
        count++;
    }

    public long getMessageId() {
        return messageId;
    }
}
