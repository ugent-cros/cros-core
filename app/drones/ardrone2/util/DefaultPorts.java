package drones.ardrone2.util;

/**
 * Created by brecht on 3/24/15.
 */
public enum DefaultPorts {
    FTP(5551),
    AUTH_PORT(5552),
    VIDEO_RECORDER_PORT(5553),
    NAV_DATA(5554),
    VIDEO_DATA(5555),
    AT_COMMAND(5556),
    RAW_CAPTURE_PORT(5557),
    PRINTF_PORT(5558),
    CONTROL_PORT(5559);

    private int port;

    private DefaultPorts(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
