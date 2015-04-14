package drones.models.scheduler.messages;

/**
 * Created by Ronald on 13/04/2015.
 */
public class DroneAddedMessage {

    private long droneId;

    public DroneAddedMessage(long droneId){
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
