package drones.models.scheduler.messages;

/**
 * Created by Ronald on 13/04/2015.
 */
public class DroneRemovedMessage {

    private long droneId;

    public DroneRemovedMessage(long droneId){
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
