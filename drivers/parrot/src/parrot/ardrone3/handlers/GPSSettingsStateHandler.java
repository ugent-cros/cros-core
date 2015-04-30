package parrot.ardrone3.handlers;

import akka.util.ByteIterator;
import parrot.ardrone3.models.CommandProcessor;
import parrot.ardrone3.models.Packet;
import parrot.ardrone3.util.FrameHelper;
import messages.GPSFixChangedMessage;
import parrot.messages.HomeChangedMessage;

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
