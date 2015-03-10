package drones.util.ardrone3;

import akka.util.ByteStringBuilder;
import drones.handlers.ardrone3.ArDrone3TypeProcessor;
import drones.handlers.ardrone3.CommonTypeProcessor;
import drones.models.ardrone3.Packet;
import drones.models.ardrone3.PacketType;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PacketCreator {

    public static Packet createFlatTrimPacket(){
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)0, null);
    }

    public static Packet createTakeOffPacket(){
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)1, null);
    }

    public static Packet createLandingPacket(){
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)3, null);
    }

    public static Packet createRequestStatusPacket(){
        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.COMMON.getVal(), (short)0, null);
    }

    public static Packet createRequestAllSettingsCommand(){
        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.SETTINGS.getVal(), (short)0, null);
    }

    public static Packet createOutdoorStatusPacket(boolean outdoor){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(outdoor ? (byte)1 : (byte)0);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.SPEEDSETTINGS.getVal(), (short)3, b.result());
    }
}
