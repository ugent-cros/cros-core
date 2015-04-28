package drones.util.ardrone3;

import akka.util.ByteStringBuilder;
import drones.handlers.ardrone3.ArDrone3TypeProcessor;
import drones.handlers.ardrone3.CommonTypeProcessor;
import drones.models.ardrone3.Packet;
import drones.models.ardrone3.PacketType;
import model.properties.FlipType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.nio.charset.Charset;

/**
 * Created by Cedric on 3/8/2015.
 */
public class PacketCreator {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMAT = ISODateTimeFormat.basicTTimeNoMillis();

    public static Packet createFlatTrimPacket(){
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)0, null);
    }

    public static Packet createTakeOffPacket(){
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)1, null);
    }

    public static Packet createLandingPacket(){
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)3, null);
    }

    public static Packet createRequestStatusPacket(){
        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.COMMON.getVal(), (short)0, null);
    }

    public static Packet createRequestAllSettingsCommand(){
        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.SETTINGS.getVal(), (short)0, null);
    }

    public static Packet createOutdoorStatusPacket(boolean outdoor){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(outdoor ? (byte)1 : (byte)0);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.SPEEDSETTINGS.getVal(), (short)3, b.result());
    }

    public static Packet createSetMaxAltitudePacket(float meters){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putFloat(meters, FrameHelper.BYTE_ORDER);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTINGSETTINGS.getVal(), (short)0, b.result());
    }

    public static Packet createSetMaxTiltPacket(float degrees){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putFloat(degrees, FrameHelper.BYTE_ORDER);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTINGSETTINGS.getVal(), (short)1, b.result());
    }

    public static Packet createMove3dPacket(boolean useRoll, byte roll, byte pitch, byte yaw, byte gaz){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(useRoll ? (byte)1 : (byte)0);
        b.putByte(roll); //Following bytes are signed! [-100;100]
        b.putByte(pitch);
        b.putByte(yaw);
        b.putByte(gaz);
        b.putFloat(0f, FrameHelper.BYTE_ORDER); //unused PSI heading for compass
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)2, b.result());
    }

    public static Packet createSetVideoStreamingStatePacket(boolean enabled){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(enabled ? (byte) 1 : (byte) 0);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.MEDIASTREAMING.getVal(), (short)0, b.result());
    }

    public static Packet createSetHullPacket(boolean hull){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(hull ? (byte) 1 : (byte) 0);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.SPEEDSETTINGS.getVal(), (short)2, b.result());
    }

    public static Packet createSetCountryPacket(String country){
        if(country == null || country.length() != 2)
            throw new IllegalArgumentException("country is in invalid format.");

        ByteStringBuilder b = new ByteStringBuilder();
        byte[] val = country.toUpperCase().getBytes(Charset.forName("UTF-8")); //enforce uppercase country code if forgotten
        b.putBytes(val);
        b.putByte((byte)0); //null terminated string

        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.SETTINGS.getVal(), (short)3, b.result());
    }

    public static Packet createSetHomePacket(double latitude, double longitude, double altitude){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putDouble(latitude, FrameHelper.BYTE_ORDER);
        b.putDouble(longitude, FrameHelper.BYTE_ORDER);
        b.putDouble(altitude, FrameHelper.BYTE_ORDER);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.GPSSETTINGS.getVal(), (short)0, b.result());
    }

    public static Packet createNavigateHomePacket(boolean start){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putByte(start ? (byte) 1 : (byte) 0);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.PILOTING.getVal(), (short)5, b.result());
    }

    public static String getDateString(DateTime date){
        return date.toString(DATE_FORMAT);
    }

    public static String getTimeString(DateTime time){
        return time.toString(TIME_FORMAT);
    }

    public static Packet createCurrentDatePacket(DateTime date){
        String content = getDateString(date);
        ByteStringBuilder b = new ByteStringBuilder();
        b.putBytes(content.getBytes(Charset.forName("UTF-8")));
        b.putByte((byte)0);

        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.COMMON.getVal(), (short)1, b.result());
    }

    public static Packet createCurrentTimePacket(DateTime time){
        String content = getTimeString(time);
        ByteStringBuilder b = new ByteStringBuilder();
        b.putBytes(content.getBytes(Charset.forName("UTF-8")));
        b.putByte((byte)0);

        return new Packet(PacketType.COMMON.getVal(), CommonTypeProcessor.CommonClass.COMMON.getVal(), (short)2, b.result());
    }

    private static int getFlipNum(FlipType type){
        switch(type){
            case FRONT:
                return 0;
            case BACK:
                return 1;
            case RIGHT:
                return 2;
            case LEFT:
                return 3;
            default:
                throw new IllegalArgumentException("fliptype");
        }
    }

    public static Packet createFlipPacket(FlipType flip){
        ByteStringBuilder b = new ByteStringBuilder();
        b.putInt(getFlipNum(flip), FrameHelper.BYTE_ORDER);
        return new Packet(PacketType.ARDRONE3.getVal(), ArDrone3TypeProcessor.ArDrone3Class.ANIMATIONS.getVal(), (short)0, b.result());
    }
}
