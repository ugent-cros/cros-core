package parrot.ardrone3.handlers;

import akka.util.ByteIterator;
import parrot.ardrone3.models.CommandProcessor;
import parrot.ardrone3.models.Packet;
import parrot.ardrone3.util.PacketHelper;
import droneapi.messages.ProductVersionChangedMessage;

/**
 * Created by Cedric on 3/10/2015.
 */
public class SettingsStateHandler extends CommandProcessor {

    /*
     ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_ALLSETTINGSCHANGED = 0,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_RESETCHANGED,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_PRODUCTNAMECHANGED,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_PRODUCTVERSIONCHANGED,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_PRODUCTSERIALHIGHCHANGED,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_PRODUCTSERIALLOWCHANGED,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_COUNTRYCHANGED,
    ARCOMMANDS_ID_COMMON_SETTINGSSTATE_CMD_AUTOCOUNTRYCHANGED,
     */

    public SettingsStateHandler(){
        super(CommonTypeProcessor.CommonClass.SETTINGSSTATE.getVal());
    }

    @Override
    protected void initHandlers() {
        addHandler((short)3, SettingsStateHandler::productVersionChanged);
    }

    private static Object productVersionChanged(Packet packet){
        ByteIterator b = packet.getData().iterator();
        String software = PacketHelper.readString(b);
        String hardware = PacketHelper.readString(b);
        return new ProductVersionChangedMessage(software, hardware);
    }
}
