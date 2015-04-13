package drones.models.scheduler.messages;

/**
 * Created by Ronald on 13/04/2015.
 */
public class DroneAddedMessage {

    private Long droneId;

    public DroneAddedMessage(Long droneId){
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}
