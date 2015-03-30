package drones.util.ardrone2;

import akka.util.ByteString;
import drones.commands.ardrone2.atcommand.ATCommand;

/**
 * Created by brecht on 3/9/15.
 */
public class PacketHelper {
    /**
     *
     * @param data
     * @param offset
     * @return
     */
    public static int getInt(byte[] data, int offset) {
        int tmp = 0, n = 0;

        for (int i=3; i>=0; i--) {
            n <<= 8;
            tmp = data[offset + i] & 0xFF;
            n |= tmp;
        }

        return n;
    }

    /**
     *
     * @param data
     * @param offset
     * @return
     */
    public static float getFloat(byte[] data, int offset) {
        return Float.intBitsToFloat(getInt(data, offset));
    }

    public static ByteString buildPacket(ATCommand packet) {
        return ByteString.fromString(packet.toString());
    }
}
