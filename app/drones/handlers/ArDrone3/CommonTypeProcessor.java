package drones.handlers.ardrone3;

import drones.models.ardrone3.CommandTypeProcessor;
import drones.models.ardrone3.PacketType;

/**
 * Created by Cedric on 3/8/2015.
 */
public class CommonTypeProcessor extends CommandTypeProcessor {

    public enum CommonClass {
        NETWORK(0),
        NETWORKEVENT(1),
        SETTINGS(2),
        SETTINGSSTATE(3),
        COMMON(4),
        COMMONSTATE(5),
        OVERHEAT(6),
        OVERHEATSTATE(7),
        CONTROLLERSTATE(8),
        WIFISETTINGS(9),
        WIFISETTINGSSTATE(10),
        MAVLINK(11),
        MAVLINKSTATE(12),
        CALIBRATION(13),
        CALIBRATIONSTATE(14),
        CAMERASETTINGSSTATE(15),
        GPS(16),
        FLIGHTPLANSTATE(17),
        FLIGHTPLANEVENT(19),
        ARLIBSVERSIONSSTATE(18);

        private byte cl;
        private CommonClass(int val) {
            this.cl = (byte) val;
        }

        public byte getVal() {
            return cl;
        }
    }

    public CommonTypeProcessor() {
        super(PacketType.COMMON.getVal());
    }

    @Override
    protected void initHandlers() {
         addCommandClassHandler(CommonClass.COMMONSTATE.getVal(), new CommonStateHandler());
    }
}
