package drones.util.ardrone2;

import akka.util.ByteString;
import drones.commands.ardrone2.atcommand.ATCommand;
import drones.commands.ardrone2.atcommand.ATCommandREF;

/**
 * Factory class for packets (ATCommands)
 * @TODO change ByteString -> ATCommand
 *
 * Created by brecht on 3/9/15.
 */
public class PacketCreator {
    public static ByteString createPacket(ATCommand atcommand) {
        return ByteString.fromString(atcommand.toString());
    }
}
