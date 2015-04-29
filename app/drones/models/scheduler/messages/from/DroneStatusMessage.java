package drones.models.scheduler.messages.from;

import api.DroneStatus;
import models.Drone;

/**
 * Created by Ronald on 29/04/2015.
 */
public class DroneStatusMessage implements SchedulerEvent{

    private long droneId;
    private Drone.Status status;

    public DroneStatusMessage(long droneId, Drone.Status status) {
        this.droneId = droneId;
        this.status = status;
    }

    public long getDroneId() {
        return droneId;
    }

    public Drone.Status getDroneStatus() {
        return status;
    }
}
