package drones.handlers.ArDrone3;

import akka.util.ByteIterator;
import drones.messages.*;
import drones.models.CommandProcessor;
import drones.models.FlyingState;
import drones.models.Packet;
import drones.models.PacketType;
import drones.util.FrameHelper;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PilotingStateHandler extends CommandProcessor {

    public final static byte PACKET_TYPE = PacketType.ARDRONE3.getNum();
    public final static byte COMMAND_CLASS = 4;

    @Override
    protected void initHandlers() {
        //TODO: use annotations

        addHandler((short)0, PilotingStateHandler::flatTrimChanged);
        addHandler((short)1, PilotingStateHandler::flyingStateChanged);
        addHandler((short)4, PilotingStateHandler::positionChanged);
        addHandler((short)5, PilotingStateHandler::speedChanged);
        addHandler((short)6, PilotingStateHandler::attitudeChanged);
        addHandler((short)8, PilotingStateHandler::altitudeChanged);
    }

    public static Object flatTrimChanged(Packet p){
        return new FlatTrimChangedMessage();
    }

    public static Object attitudeChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        float roll = it.getFloat(FrameHelper.BYTE_ORDER);
        float pitch = it.getFloat(FrameHelper.BYTE_ORDER);
        float yaw = it.getFloat(FrameHelper.BYTE_ORDER);
        return new AttitudeChangedMessage(roll, pitch, yaw);
    }

    public static Object speedChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        float sx = it.getFloat(FrameHelper.BYTE_ORDER);
        float sy = it.getFloat(FrameHelper.BYTE_ORDER);
        float sz = it.getFloat(FrameHelper.BYTE_ORDER);

        return new SpeedChangedMessage(sx, sy, sz);
    }

    private static FlyingState getFlyingState(int val){
        switch(val){
            case 0:
                return FlyingState.LANDED;
            case 1:
                return FlyingState.TAKINGOFF;
            case 2:
                return FlyingState.HOVERING;
            case 3:
                return FlyingState.FLYING;
            case 4:
                return FlyingState.LANDING;
            case 5:
                return FlyingState.EMERGENCY;
            default:
                throw new IllegalArgumentException("val");
        }
    }

    public static Object flyingStateChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        int val = it.getInt(FrameHelper.BYTE_ORDER);
        return new FlyingStateChangedMessage(getFlyingState(val));
    }

    public static Object altitudeChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);
        return new AltitudeChangedMessage(alt);
    }

    public static Object positionChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        double lat = it.getDouble(FrameHelper.BYTE_ORDER);
        double longit = it.getDouble(FrameHelper.BYTE_ORDER);
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);

        if(lat == 500.0d)
            lat = PositionChangedMessage.UNAVAILABLE;
        if(longit == 500.0d)
            longit = PositionChangedMessage.UNAVAILABLE;

        return new PositionChangedMessage(longit, lat, alt);

    }
}
