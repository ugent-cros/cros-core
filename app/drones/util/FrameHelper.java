package drones.util;

import drones.models.FrameType;

/**
 * Created by Cedric on 3/6/2015.
 */
public class FrameHelper {

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
}
