package drones.handlers.ardrone3;

import akka.util.ByteIterator;
import drones.models.ardrone3.CommandProcessor;
import drones.models.ardrone3.Packet;
import drones.util.ardrone3.FrameHelper;
import messages.GPSFixChangedMessage;
import messages.HomeChangedMessage;

/**
 * Created by Cedric on 3/20/2015.
 */
public class GPSSettingsStateHandler extends CommandProcessor {

    public GPSSettingsStateHandler(){
        super(ArDrone3TypeProcessor.ArDrone3Class.GPSSETTINGSSTATE.getVal());
    }

    @Override
    protected void initHandlers() {
        addHandler((short)0, GPSSettingsStateHandler::homeChanged);
        addHandler((short)2, GPSSettingsStateHandler::gpsFixChanged);
    }

    private static Object homeChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        double lat = it.getDouble(FrameHelper.BYTE_ORDER);
        double lon = it.getDouble(FrameHelper.BYTE_ORDER);
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);
        return new HomeChangedMessage(lat, lon, alt);
    }

    private static Object gpsFixChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        boolean fixed = it.getByte() == 1;
        return new GPSFixChangedMessage(fixed);
    }
}
