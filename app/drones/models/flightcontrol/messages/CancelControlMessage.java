package drones.models.flightcontrol.messages;

/**
 * Message to cancel a flightcontrol
 *
 * Created by Sander on 16/04/2015.
 */
public class CancelControlMessage {

    private Long droneId;

    public CancelControlMessage(Long droneId) {
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}