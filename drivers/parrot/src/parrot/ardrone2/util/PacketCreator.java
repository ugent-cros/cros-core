package parrot.ardrone2.util;

import akka.util.ByteString;
import parrot.ardrone2.commands.ATCommand;

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
