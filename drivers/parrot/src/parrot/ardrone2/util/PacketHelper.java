package parrot.ardrone2.util;

import akka.util.ByteString;
import parrot.ardrone2.commands.ATCommand;

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
        int value = 0;
        for (int i = 3; i >= 0; i--) {
            int shift = i * 8;
            value += (data[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }


    /**
     *
     * @param data
     * @param offset
     * @return
     */
    public static long getLong(byte[] data, int offset) {
        long value = 0;
        for (int i = 7; i >= 0; i--) {
            int shift = i * 8;
            value += (data[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    /**
     *
     * @param data
     * @param offset
     * @return
     */
    public static int getShort(byte[] data, int offset) {
        return ((data[offset + 1] & 0x000000FF) << 8) + (data[offset] & 0x000000FF);
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

    /**
     *
     * @param data
     * @param offset
     * @return
     */
    public static double getDouble(byte[] data, int offset) {
        return Double.longBitsToDouble(getLong(data, offset));
    }

    public static ByteString buildPacket(ATCommand packet) {
        return ByteString.fromString(packet.toString());
    }
}
