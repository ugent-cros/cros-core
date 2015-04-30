package parrot.ardrone3.util;

import akka.util.ByteIterator;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import parrot.ardrone3.models.Packet;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PacketHelper {

    public static ByteString buildPacket(Packet packet){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(packet.getType());
        b.putByte(packet.getCommandClass());
        b.putShort(packet.getCommand(), FrameHelper.BYTE_ORDER);

        if(packet.getData() == null)
            return b.result();
        else
            return b.result().concat(packet.getData());
    }

    public static ByteString getPingPacket(long epoch){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putLong(epoch, FrameHelper.BYTE_ORDER);
        return b.result();
    }

    public static String readString(ByteIterator it){
        // Reads null terminated string
        StringBuilder b = new StringBuilder();
        byte v = it.getByte();
        while(v != 0){
            b.append((char)v);
            v = it.getByte();
        }
        return b.toString();
    }
}
