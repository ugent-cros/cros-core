package parrot.ardrone3.models;

import akka.util.ByteString;

/**
 * Created by Cedric on 3/8/2015.
 */
public class Packet {
    private byte type;
    private byte commandClass;
    private short command;

    private ByteString data;

    public Packet(byte type, byte commandClass, short command, ByteString data) {
        this.type = type;
        this.commandClass = commandClass;
        this.command = command;
        this.data = data;
    }

    public byte getType() {
        return type;
    }

    public byte getCommandClass() {
        return commandClass;
    }

    public short getCommand() {
        return command;
    }

    public ByteString getData() {
        return data;
    }

}
