package drones.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */

public class DroneCommandMessage implements Serializable {

    private Object message;

    public DroneCommandMessage(Object message){
        this.message = message;
    }

    public Object getMessage(){
        return message;
    }
}
