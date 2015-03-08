package drones.models.ardrone3;

/**
 * Created by Cedric on 3/8/2015.
 */
public enum PacketType {

    ARDRONE3(1),
    ARDRONE3DEBUG(129),
    JUMPINGSUMO(3),
    JUMPINGSUMODEBUG(131),
    MINIDRONE(2),
    MINIDRONEDEBUG(130),
    SKYCONTROLLER(4),
    SKYCONTROLLERDEBUG(132),
    COMMON(0),
    COMMONDEBUG(128);

    private byte num;
    private PacketType(int type){
        this.num = (byte)type;
    }

    public byte getVal(){
        return num;
    }
}
