package drones.models.scheduler.messages.from;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Ronald on 16/04/2015.
 */
public class SchedulerScheduledMessage implements SchedulerEvent{

    private List<Long> assignmentIds;

    public SchedulerScheduledMessage(List<Long> assignmentIds) {
        this.assignmentIds = assignmentIds;
    }

    public List<Long> getAssignmentIds() {
        return assignmentIds;
    }
}
