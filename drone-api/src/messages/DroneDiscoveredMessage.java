package messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class DroneDiscoveredMessage implements Serializable {

    public enum DroneDiscoveryStatus {
        SUCCESS,
        FAILED
    }

    private final int sendPort;
    private final int recvPort;
    private final int fragmentSize;
    private final int maxFragmentNum;
    private final int maxAckInterval;
    private final int updatePort;
    private final int userPort;
    private final DroneDiscoveryStatus status;

    public DroneDiscoveredMessage(DroneDiscoveryStatus status, int sendPort, int recvPort, int fragmentSize, int maxFragmentNum, int maxAckInterval, int updatePort, int userPort) {
        this.status = status;
        this.sendPort = sendPort;
        this.recvPort = recvPort;
        this.fragmentSize = fragmentSize;
        this.maxFragmentNum = maxFragmentNum;
        this.maxAckInterval = maxAckInterval;
        this.updatePort = updatePort;
        this.userPort = userPort;
    }

    public DroneDiscoveredMessage(DroneDiscoveryStatus status){
        this(status, 54321, 43210, 1000, 128, 0, 51, 21);
    }

    public DroneDiscoveryStatus getStatus() {
        return status;
    }

    public int getSendPort() {
        return sendPort;
    }

    public int getRecvPort() {
        return recvPort;
    }

    public int getFragmentSize() {
        return fragmentSize;
    }

    public int getMaxFragmentNum() {
        return maxFragmentNum;
    }

    public int getMaxAckInterval() {
        return maxAckInterval;
    }

    public int getUpdatePort() {
        return updatePort;
    }

    public int getUserPort() {
        return userPort;
    }
}
