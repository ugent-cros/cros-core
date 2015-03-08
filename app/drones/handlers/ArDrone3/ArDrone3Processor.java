package drones.handlers.ardrone3;

import drones.models.ardrone3.CommandTypeProcessor;
import drones.models.ardrone3.PacketType;

/**
 * Created by Cedric on 3/8/2015.
 */
public class ArDrone3Processor extends CommandTypeProcessor {

    public ArDrone3Processor() {
        super(PacketType.ARDRONE3.getNum());
    }

    @Override
    protected void initHandlers() {
        addCommandClassHandler(PilotingStateHandler.COMMAND_CLASS, new PilotingStateHandler());
    }
}
