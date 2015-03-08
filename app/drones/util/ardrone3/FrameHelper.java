package drones.util.ardrone3;

import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import drones.models.ardrone3.Frame;
import drones.models.ardrone3.FrameType;

import java.nio.ByteOrder;

/**
 * Created by Cedric on 3/6/2015.
 */
public class FrameHelper {

    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private final static int MAX_WIFI_ID = 256;

    //TODO: try to embed frame type parse code in enum itself
    /***
     * Converts a frame byte to an enum value
     * @param type The byte value
     * @return The enum value
     */
    public static FrameType parseFrameType(byte type){
        switch(type){
            case 1:
                return FrameType.ACK;
            case 2:
                return FrameType.DATA;
            case 3:
                return FrameType.DATA_LOW_LATENCY;
            case 4:
                return FrameType.DATA_WITH_ACK;
            default:
                throw new IllegalArgumentException("type");
        }
    }

    /***
     * Calculates the acknowledged id from the server to the drone
     * @param id The id to acknowledge
     * @return The id to send
     */
    public static byte getAckToDrone(byte id){
        return (byte)(id + (MAX_WIFI_ID/2));
    }

    /***
     * Calculates the acknowledged id from a drone to the server id's
     * @param id The received id
     * @return The id to acknowledge
     */
    public static byte getAckToServer(byte id){
        return (byte)(id - (MAX_WIFI_ID/2));
    }

    public static ByteString getAck(Frame frame){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(frame.getSeq());
        return b.result();
    }

    public static ByteString getPong(long time){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putLong(time, BYTE_ORDER);
        b.putLong(0, BYTE_ORDER); //add garbage nanoseconds
        return b.result();
    }

    public static ByteString getFrameData(Frame frame){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(frame.getType().getByte());
        b.putByte(frame.getId());
        b.putByte(frame.getSeq());

        b.putInt(frame.getData().length(), BYTE_ORDER);

        return b.result().concat(frame.getData());
    }
}
