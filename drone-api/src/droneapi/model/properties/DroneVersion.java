package droneapi.model.properties;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class DroneVersion implements Serializable {
    private String software;
    private String hardware;

    public DroneVersion(String software, String hardware) {
        this.software = software;
        this.hardware = hardware;
    }

    public String getSoftware() {
        return software;
    }

    public String getHardware() {
        return hardware;
    }
}
