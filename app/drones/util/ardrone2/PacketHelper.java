package drones.util.ardrone2;

import akka.util.ByteString;
import drones.commands.ArDrone2.ATCommand.ATCommand;

/**
 * Created by brecht on 3/9/15.
 */
public class PacketHelper {
    public static ByteString buildPacket(ATCommand packet) {
        return ByteString.fromString(packet.toString());
    }
}
