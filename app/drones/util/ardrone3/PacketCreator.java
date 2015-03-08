package drones.util.ardrone3;

import drones.handlers.ardrone3.ArDrone3TypeProcessor;
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
}
