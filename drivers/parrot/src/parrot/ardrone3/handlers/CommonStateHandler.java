package parrot.ardrone3.handlers;

import akka.util.ByteIterator;
import parrot.ardrone3.models.CommandProcessor;
import parrot.ardrone3.models.Packet;
import droneapi.messages.BatteryPercentageChangedMessage;

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
