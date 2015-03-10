package drones.util.ardrone2;

import akka.util.ByteString;
import drones.commands.ArDrone2.ATCommand.ATCommandREF;

/**
 * Factory class for packets (ATCommands)
 * @TODO change ByteString -> ATCommand
 *
 * Created by brecht on 3/9/15.
 */
public class PacketCreator {

    // REF COMMANDS
    /**
     *
     * @return ByteString of Take-off command
     */
    public static ByteString createTakeOffPacket() {
        // The 9th bit is the landing bit
        int value = (1 << 9) | (1 << 18) | (1 << 20) | (1 << 22) | (1 << 24) | (1 << 28);
        return ByteString.fromString(new ATCommandREF(value).toString());
    }

    /**
     *
     * @return ByteString of Landing command
     */
    public static ByteString createLandingPacket() {
        int value = (1 << 18) | (1 << 20) | (1 << 22) | (1 << 24) | (1 << 28);
        return ByteString.fromString(new ATCommandREF(value).toString());
    }

    /**
     *
     * @return ByteString of Emergency command
     */
    public static ByteString createEmergencyPacket() {
        // The 8th bit is the emergency bit
        int value = (1 << 8) | (1 << 18) | (1 << 20) | (1 << 22) | (1 << 24) | (1 << 28);
        return ByteString.fromString(new ATCommandREF(value).toString());
    }
}
