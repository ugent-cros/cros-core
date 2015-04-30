package droneapi.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/22/2015.
 */
public class CalibrateRequestMessage implements Serializable {
    boolean hull;
    boolean outdoor;

    public CalibrateRequestMessage(boolean hull, boolean outdoor) {
        this.hull = hull;
        this.outdoor = outdoor;
    }

    public boolean hasHull() {
        return hull;
    }

    public boolean isOutdoor() {
        return outdoor;
    }
}
