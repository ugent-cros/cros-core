package parrot.ardrone3.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class CommandTypeProcessor {

    private final Map<Byte, CommandProcessor> processors;
    private byte type;

    public CommandTypeProcessor(byte type){
        this.type = type;
        this.processors = new HashMap<>();

        initHandlers();
    }

    protected abstract void initHandlers();

    public byte getType(){
        return type;
    }

    protected void addCommandClassHandler(byte cmdClass, CommandProcessor p){
        processors.put(cmdClass, p);
    }

    public Object handle(Packet p){
        if(p.getType() != type){
            throw new IllegalArgumentException("Invalid packet type routing.");
        }
        CommandProcessor c = processors.get(p.getCommandClass());
        if(c == null)
            return null;

        return c.handle(p);
    }


}
