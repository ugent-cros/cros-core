package drones.scheduler.messages.to;

/**
 * Created by Ronald on 17/04/2015.
 */
public class FlightCanceledMessage {

    private long droneId;

    public FlightCanceledMessage(long droneId) {
        this.droneId = droneId;
    }

    public long getDroneId() {
        return droneId;
    }
}
