package drones.util.ardrone2;

import akka.util.ByteString;
import drones.commands.ardrone2.atcommand.ATCommand;

/**
 * Created by brecht on 3/9/15.
 */
public class PacketHelper {
    public static ByteString buildPacket(ATCommand packet) {
        return ByteString.fromString(packet.toString());
    }
}
