package drones.util.ardrone2;

import akka.util.ByteString;
import drones.commands.ardrone2.atcommand.ATCommand;

/**
 * Factory class for packets (ATCommands)
 *
 * Created by brecht on 3/9/15.
 */
public class PacketCreator {
    public static ByteString createPacket(ATCommand atcommand) {
        return ByteString.fromString(atcommand.toString());
    }
}
