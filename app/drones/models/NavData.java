package drones.models;

/**
 * See https://code.google.com/p/ardroneme/source/browse/trunk/NavData.java
 *  Code is originally from MAPGPS
 *
 *
 * Created by brecht on 3/8/15.
 */
public class NavData {
    private static final int NAV_PORT           = 5554;

    // OFFSETS
    private static final int NAV_STATE_OFFSET   =  4;
    private static final int NAV_BATTERY_OFFSET = 24;
    private static final int NAV_PITCH_OFFSET   = 28;
    private static final int NAV_ROLL_OFFSET    = 32;
    private static final int NAV_YAW_OFFSET     = 36;
    private static final int NAV_ALTITUDE_OFFSET= 40;
    //Reuse some fields for NavData2 from Arduino sensor board with GPS/Compass/Barometer
    private static final int NAV_LATITUDE_OFFSET          = 44;
    private static final int NAV_LONGITUDE_OFFSET         = 48;
    private static final int NAV_HEADING_OFFSET           = 52;
    private static final int NAV_ALTITUDE_US_OFFSET       = 56;
    private static final int NAV_ALTITUDE_BARO_OFFSET     = 60;
    private static final int NAV_ALTITUDE_BARO_RAW_OFFSET = 64;

    // @TODO check what these masks do
    private static final int MYKONOS_TRIM_COMMAND_MASK   = 1 <<  7; /*!< Trim command ACK : (0) None, (1) one received */
    private static final int MYKONOS_TRIM_RUNNING_MASK   = 1 <<  8; /*!< Trim running : (0) none, (1) running */
    private static final int MYKONOS_TRIM_RESULT_MASK    = 1 <<  9; /*!< Trim result : (0) failed, (1) succeeded */
    private static final int MYKONOS_ANGLES_OUT_OF_RANGE = 1 << 19; /*!< Angles : (0) Ok, (1) out of range */
    private static final int MYKONOS_WIND_MASK           = 1 << 20; /*!< Wind : (0) Ok, (1) too much to fly */
    private static final int MYKONOS_ULTRASOUND_MASK     = 1 << 21; /*!< Ultrasonic sensor : (0) Ok, (1) deaf */
    private static final int MYKONOS_CUTOUT_MASK         = 1 << 22; /*!< Cutout system detection : (0) Not detected, (1) detected */
    private static final int MYKONOS_COM_WATCHDOG_MASK   = 1 << 30; /*!< Communication Watchdog : (1) com problem, (0) Com is ok */
    private static final int MYKONOS_EMERGENCY_MASK      = 1 << 31; /*!< Emergency landing : (0) no emergency, (1) emergency */

    private byte[] trigger_bytes = {0x01, 0x00, 0x00, 0x00};
    private float attitude_pitch, attitude_roll, attitude_yaw;


    // HELPER METHODS
    /**
     *
     * @param data
     * @param offset
     * @return
     */
    private int getInt(byte[] data, int offset) {
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
    private float getFloat(byte[] data, int offset) {
        return Float.intBitsToFloat(getInt(data, offset));
    }

}
