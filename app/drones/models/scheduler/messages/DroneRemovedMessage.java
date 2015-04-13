package drones.models.scheduler.messages;

/**
 * Created by Ronald on 13/04/2015.
 */
public class DroneRemovedMessage {

    private Long droneId;

    public DroneRemovedMessage(Long droneId){
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}
