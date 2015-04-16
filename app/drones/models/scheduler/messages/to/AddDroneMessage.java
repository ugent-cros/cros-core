package drones.models.scheduler.messages.to;

/**
 * Created by Ronald on 13/04/2015.
 */
public class AddDroneMessage {

    private long droneId;

    public AddDroneMessage(long droneId){
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
