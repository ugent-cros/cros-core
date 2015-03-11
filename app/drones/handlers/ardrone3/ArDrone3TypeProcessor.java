package drones.handlers.ardrone3;

import drones.models.ardrone3.CommandTypeProcessor;
import drones.models.ardrone3.PacketType;

/**
 * Created by Cedric on 3/8/2015.
 */
public class ArDrone3TypeProcessor extends CommandTypeProcessor {

    public enum ArDrone3Class {
        PILOTING(0),
        ANIMATIONS(5),
        CAMERA(1),
        MEDIARECORD(7),
        MEDIARECORDSTATE(8),
        MEDIARECORDEVENT(3),
        PILOTINGSTATE(4),
        NETWORK(13),
        NETWORKSTATE(14),
        PILOTINGSETTINGS(2),
        PILOTINGSETTINGSSTATE(6),
        SPEEDSETTINGS(11),
        SPEEDSETTINGSSTATE(12),
        NETWORKSETTINGS(9),
        NETWORKSETTINGSSTATE(10),
        SETTINGS(15),
        SETTINGSSTATE(16),
        DIRECTORMODE(17),
        DIRECTORMODESTATE(18),
        PICTURESETTINGS(19),
        PICTURESETTINGSSTATE(20),
        MEDIASTREAMING(21),
        MEDIASTREAMINGSTATE(22),
        GPSSETTINGS(23),
        GPSSETTINGSSTATE(24),
        CAMERASTATE(25),
        ANTIFLICKERING(29);

        private byte cl;
        private ArDrone3Class(int val){
            this.cl = (byte)val;
        }

        public byte getVal(){
            return cl;
        }
    }

    public ArDrone3TypeProcessor() {
        super(PacketType.ARDRONE3.getVal());
    }

    @Override
    protected void initHandlers() {
        addCommandClassHandler(ArDrone3Class.PILOTINGSTATE.getVal(), new PilotingStateHandler());
    }
}
