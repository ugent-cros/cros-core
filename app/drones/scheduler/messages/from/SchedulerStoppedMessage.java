package drones.scheduler.messages.from;

/**
 * Created by Ronald on 16/04/2015.
 */
public class SchedulerStoppedMessage implements SchedulerEvent{

    @Override
    public String toString() {
        return "Scheduler has stopped successfully.";
    }
}
