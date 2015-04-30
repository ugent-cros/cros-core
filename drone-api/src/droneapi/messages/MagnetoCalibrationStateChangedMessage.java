package droneapi.messages;

import java.io.Serializable;

/**
 * Created by Cedric on 4/6/2015.
 */
public class MagnetoCalibrationStateChangedMessage implements Serializable {
    private boolean calibrationRequired;

    public MagnetoCalibrationStateChangedMessage(boolean calibrationRequired) {
        this.calibrationRequired = calibrationRequired;
    }

    public boolean isCalibrationRequired() {
        return calibrationRequired;
    }
}
