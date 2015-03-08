package drones.util.ardrone3;

import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import drones.models.ardrone3.Packet;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PacketHelper {

    public static ByteString buildPacket(Packet packet){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(packet.getType());
        b.putByte(packet.getCommandClass());
        b.putShort(packet.getCommand(), FrameHelper.BYTE_ORDER);

        return b.result().concat(packet.getData());
    }
}
