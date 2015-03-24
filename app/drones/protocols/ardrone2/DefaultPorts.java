package drones.protocols.ardrone2;

/**
 * Created by brecht on 3/24/15.
 */
public enum DefaultPorts {
    NAV_DATA(5554),
    VIDEO_DATA(5555),
    AT_COMMAND(5556);

    private int port;

    private DefaultPorts(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
