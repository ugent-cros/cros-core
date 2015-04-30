package drones.scheduler.messages.from;

/**
 * Created by Ronald on 30/04/2015.
 */
public class AssignmentProgressedMessage implements SchedulerEvent{

    private long assignmentId;
    private int progress;

    public AssignmentProgressedMessage(long assignmentId, int progress) {
        this.assignmentId = assignmentId;
        this.progress = progress;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public int getProgress() {
        return progress;
    }
}
