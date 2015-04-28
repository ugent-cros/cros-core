package messages;

import java.io.Serializable;

/**
 * Created by Benjamin on 14/04/2015.
 */
public class DroneAssignedMessage implements Serializable {
    private long assignedDroneID;

    public DroneAssignedMessage(long id) {
        this.assignedDroneID = id;
    }

    public long getAssignedDroneID() {
        return assignedDroneID;
    }
}
