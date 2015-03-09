package drones.handlers.ardrone3;

import akka.util.ByteIterator;
import drones.messages.*;
import drones.models.ardrone3.CommandProcessor;
import drones.models.FlyingState;
import drones.models.ardrone3.Packet;
import drones.util.ardrone3.FrameHelper;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PilotingStateHandler extends CommandProcessor {

    public PilotingStateHandler(){
        super(ArDrone3TypeProcessor.ArDrone3Class.PILOTINGSTATE.getVal());
    }

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

    private static Object flatTrimChanged(Packet p){
        return new FlatTrimChangedMessage();
    }

    private static Object attitudeChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        float roll = it.getFloat(FrameHelper.BYTE_ORDER);
        float pitch = it.getFloat(FrameHelper.BYTE_ORDER);
        float yaw = it.getFloat(FrameHelper.BYTE_ORDER);
        return new AttitudeChangedMessage(roll, pitch, yaw);
    }

    private static Object speedChanged(Packet p){
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

    private static Object flyingStateChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        int val = it.getInt(FrameHelper.BYTE_ORDER);
        return new FlyingStateChangedMessage(getFlyingState(val));
    }

    private static Object altitudeChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);
        return new AltitudeChangedMessage(alt);
    }

    private static Object positionChanged(Packet p){
        ByteIterator it = p.getData().iterator();
        double lat = it.getDouble(FrameHelper.BYTE_ORDER);
        double longit = it.getDouble(FrameHelper.BYTE_ORDER);
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);

        if(lat == 500.0d)
            lat = 0d;
        if(longit == 500.0d)
            longit = 0d;

        return new PositionChangedMessage(longit, lat, alt);

    }
}
