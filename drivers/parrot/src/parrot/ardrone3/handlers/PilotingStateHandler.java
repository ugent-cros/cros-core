package parrot.ardrone3.handlers;

import akka.util.ByteIterator;
import parrot.ardrone3.models.CommandProcessor;
import parrot.ardrone3.models.Packet;
import parrot.ardrone3.util.FrameHelper;
import messages.*;
import model.properties.AlertState;
import model.properties.FlyingState;
import model.properties.NavigationState;
import model.properties.NavigationStateReason;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PilotingStateHandler extends CommandProcessor {

    public PilotingStateHandler() {
        super(ArDrone3TypeProcessor.ArDrone3Class.PILOTINGSTATE.getVal());
    }

    @Override
    protected void initHandlers() {
        //TODO: use annotations

        addHandler((short) 0, PilotingStateHandler::flatTrimChanged);
        addHandler((short) 1, PilotingStateHandler::flyingStateChanged);
        addHandler((short) 2, PilotingStateHandler::alertStateChanged);
        addHandler((short) 3, PilotingStateHandler::navigateHomeStateChanged);
        addHandler((short) 4, PilotingStateHandler::positionChanged);
        addHandler((short) 5, PilotingStateHandler::speedChanged);
        addHandler((short) 6, PilotingStateHandler::attitudeChanged);
        addHandler((short) 8, PilotingStateHandler::altitudeChanged);
    }

    private static Object flatTrimChanged(Packet p) {
        return new FlatTrimChangedMessage();
    }

    private static Object attitudeChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        float roll = it.getFloat(FrameHelper.BYTE_ORDER);
        float pitch = it.getFloat(FrameHelper.BYTE_ORDER);
        float yaw = it.getFloat(FrameHelper.BYTE_ORDER);
        return new RotationChangedMessage(roll, pitch, yaw);
    }

    private static Object speedChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        float sx = it.getFloat(FrameHelper.BYTE_ORDER);
        float sy = it.getFloat(FrameHelper.BYTE_ORDER);
        float sz = it.getFloat(FrameHelper.BYTE_ORDER);

        return new SpeedChangedMessage(sx, sy, sz);
    }

    private static FlyingState getFlyingState(int val) {
        switch (val) {
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

    private static Object flyingStateChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        int val = it.getInt(FrameHelper.BYTE_ORDER);
        return new FlyingStateChangedMessage(getFlyingState(val));
    }

    private static AlertState getAlertState(int val) {
        switch (val) {
            case 0:
                return AlertState.NONE;
            case 1:
                return AlertState.USER_EMERGENCY;
            case 2:
                return AlertState.CUT_OUT;
            case 3:
                return AlertState.BATTERY_CRITICAL;
            case 4:
                return AlertState.BATTERY_LOW;
            case 5:
                return AlertState.ANGLE_CRITICAL;
            default:
                throw new IllegalArgumentException("val");
        }
    }

    private static NavigationState getNavigationState(int value) {
        switch (value) {
            case 0:
                return NavigationState.AVAILABLE;
            case 1:
                return NavigationState.IN_PROGRESS;
            case 2:
                return NavigationState.UNAVAILABLE;
            case 3:
                return NavigationState.PENDING;
            default:
                throw new IllegalArgumentException("value");
        }
    }

    private static NavigationStateReason getNavigationStateReason(int reason) {
        switch (reason) {
            case 0:
                return NavigationStateReason.REQUESTED;
            case 1:
                return NavigationStateReason.CONNECTION_LOST;
            case 2:
                return NavigationStateReason.BATTERY_LOW;
            case 3:
                return NavigationStateReason.FINISHED;
            case 4:
                return NavigationStateReason.STOPPED;
            case 5:
                return NavigationStateReason.DISABLED;
            case 6:
                return NavigationStateReason.ENABLED;
            default:
                throw new IllegalArgumentException("reason");
        }
    }

    private static Object navigateHomeStateChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        int state = it.getInt(FrameHelper.BYTE_ORDER);
        int reason = it.getInt(FrameHelper.BYTE_ORDER);
        return new NavigationStateChangedMessage(getNavigationState(state), getNavigationStateReason(reason));
    }

    private static Object alertStateChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        int val = it.getInt(FrameHelper.BYTE_ORDER);
        return new AlertStateChangedMessage(getAlertState(val));
    }

    private static Object altitudeChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);
        return new AltitudeChangedMessage(alt);
    }

    private static Object positionChanged(Packet p) {
        ByteIterator it = p.getData().iterator();
        double lat = it.getDouble(FrameHelper.BYTE_ORDER);
        double longit = it.getDouble(FrameHelper.BYTE_ORDER);
        double alt = it.getDouble(FrameHelper.BYTE_ORDER);

        if ((int) lat == 500) //this uses the exact 500.0d constant, but Sonar mehh
            lat = 0d;
        if ((int) longit == 500)
            longit = 0d;
        if ((int) alt == 500)
            alt = 0d;

        return new LocationChangedMessage(longit, lat, alt);

    }
}
