package drones.models.ardrone3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class CommandProcessor {

    private Map<Short, Function<Packet, Object>> handlers;
    private byte commandClass;

    public CommandProcessor(byte commandClass){
        this.handlers = new HashMap<>();
        this.commandClass = commandClass;

        initHandlers();
    }

    protected void addHandler(short command, Function<Packet, Object> handler){
        handlers.put(command, handler);
    }

    protected abstract void initHandlers();

    public Object handle(Packet p){
        if(p.getCommandClass() != commandClass){
            throw new IllegalArgumentException("Invalid packet class routing.");
        }

        Function<Packet, Object> f = handlers.get(p.getCommand());
        if(f == null)
            return null;

        return f.apply(p);
    }
}
