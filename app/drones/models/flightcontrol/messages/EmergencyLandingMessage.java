package drones.models.flightcontrol.messages;

/**
 * All drones will land without requests.
 *
 * Created by Sander on 16/04/2015.
 */
public class EmergencyLandingMessage {

    private Long droneId;

    public EmergencyLandingMessage(Long droneId) {
        this.droneId = droneId;
    }

    public Long getDroneId() {
        return droneId;
    }
}
