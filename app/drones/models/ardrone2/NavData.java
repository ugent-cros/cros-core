package drones.models.ardrone2;

/**
 * Created by brecht on 3/23/15.
 */
public enum NavData {
    // Offsets of navdata
    NAV_HEADER_OFFSET(0),
    NAV_STATE_OFFSET(4),
    NAV_CTRL_STATE_OFFSET(20),
    NAV_BATTERY_OFFSET(24),
    NAV_PITCH_OFFSET(28),
    NAV_ROLL_OFFSET(32),
    NAV_YAW_OFFSET(36),
    NAV_ALTITUDE_OFFSET(40),
    NAV_VX_OFFSET(44),
    NAV_VY_OFFSET(48),
    NAV_VZ_OFFSET(52),
    // REST OF NAVDATA_DEMO IS UNUSED
    NAV_LATITUDE_OFFSET(44), // @TODO
    NAV_LONGITUDE_OFFSET(48); // @TODO

    private int offset;

    private NavData(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
