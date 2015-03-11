package drones.models.ardrone3;

/**
 * Created by Cedric on 3/6/2015.
 */
public enum FrameType {

    ACK(1),
    DATA(2),
    DATA_LOW_LATENCY(3),
    DATA_WITH_ACK(4);

    private byte id;
    private FrameType(int id){
        this.id = (byte)id;
    }

    public byte getByte(){
        return this.id;
    }
}
