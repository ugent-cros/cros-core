package drones.handlers.ardrone3;

import drones.models.ardrone3.CommandProcessor;
import drones.models.ardrone3.Packet;
import messages.MagnetoCalibrationStateChangedMessage;

/**
 * Created by Cedric on 4/6/2015.
 */
public class CalibrationStateHandler  extends CommandProcessor {

    public CalibrationStateHandler(){
        super(CommonTypeProcessor.CommonClass.CALIBRATIONSTATE.getVal());
    }

    @Override
    protected void initHandlers() {
        addHandler((short)1, CalibrationStateHandler::magnetoCalibrationState);
    }

    private static Object magnetoCalibrationState(Packet p){
        byte val = p.getData().iterator().getByte();
        return new MagnetoCalibrationStateChangedMessage(val == 1);
    }
}
