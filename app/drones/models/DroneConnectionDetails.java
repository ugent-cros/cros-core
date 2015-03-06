package drones.models;

/**
 * Created by Cedric on 3/6/2015.
 */
public class DroneConnectionDetails {
    private String ip;
    private int sendingPort;
    private int receivingPort;

    public DroneConnectionDetails(String ip, int sendPort, int recvPort){
        this.ip = ip;
        this.receivingPort = recvPort;
        this.sendingPort = sendPort;
    }

    public String getIp() {
        return ip;
    }

    public int getSendingPort() {
        return sendingPort;
    }

    public int getReceivingPort() {
        return receivingPort;
    }
}
