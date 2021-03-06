package drones.scheduler.messages.from;

import models.Drone;

/**
 * Created by Ronald on 29/04/2015.
 */
public class DroneStatusMessage implements SchedulerEvent{

    private long droneId;
    private Drone.Status oldStatus;
    private Drone.Status newStatus;

    public DroneStatusMessage(long droneId, Drone.Status oldStatus, Drone.Status newStatus) {
        this.droneId = droneId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public long getDroneId() {
        return droneId;
    }

    public Drone.Status getOldStatus() {
        return oldStatus;
    }

    public Drone.Status getNewStatus() {
        return newStatus;
    }

    @Override
    public String toString() {
        return String.format("Drone %d status changed from %s to %s.", droneId, oldStatus, newStatus);
    }
}
