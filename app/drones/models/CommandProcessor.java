package drones.models;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by Cedric on 3/8/2015.
 */
public abstract class CommandProcessor {

    private Map<Short, Function<Packet, Object>> handlers;

    public CommandProcessor(){
        this.handlers = new HashMap<>();
        initHandlers();
    }

    protected void addHandler(short command, Function<Packet, Object> handler){
        handlers.put(command, handler);
    }

    protected abstract void initHandlers();
}
