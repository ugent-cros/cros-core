package drones.handlers.ardrone3;

import akka.util.ByteIterator;
import drones.models.ardrone3.CommandProcessor;
import drones.models.ardrone3.Packet;
import messages.BatteryPercentageChangedMessage;

/**
 * Created by Cedric on 3/8/2015.
 */
public class CommonStateHandler extends CommandProcessor {

    /*
    ALLSTATESCHANGED = 0,
    BATTERYSTATECHANGED,
    MASSSTORAGESTATELISTCHANGED,
    MASSSTORAGEINFOSTATELISTCHANGED,
    CURRENTDATECHANGED,
    CURRENTTIMECHANGED,
    MASSSTORAGEINFOREMAININGLISTCHANGED,
    WIFISIGNALCHANGED,
    SENSORSSTATESLISTCHANGED,
     */

    public CommonStateHandler(){
        super(CommonTypeProcessor.CommonClass.COMMONSTATE.getVal());
    }

    @Override
    protected void initHandlers() {
        addHandler((short)1, CommonStateHandler::batteryStateChanged);
    }

    private static Object batteryStateChanged(Packet packet){
        ByteIterator b = packet.getData().iterator();
        byte perc = b.getByte();
        return new BatteryPercentageChangedMessage(perc);
    }
}
