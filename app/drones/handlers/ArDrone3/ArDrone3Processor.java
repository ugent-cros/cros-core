package drones.handlers.ArDrone3;

import drones.models.CommandTypeProcessor;
import drones.models.PacketType;

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
