package drones.models.scheduler.messages;

/**
 * Created by Ronald on 7/04/2015.
 */
public class FleetChangedMessage {

    private int droneId;
    private Action action;

    public FleetChangedMessage(int droneId, Action action){
        this.droneId = droneId;
        this.action = action;
    }

    public int getDroneId() {
        return droneId;
    }

    public Action getAction() {
        return action;
    }

    public enum Action{
        ADDED,
        REMOVED;
    }
}
