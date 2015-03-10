package drones.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class DroneCommandMessage<T extends Serializable> implements Serializable {

    private T message;

    public DroneCommandMessage(T message){
        this.message = message;
    }

    public T getMessage(){
        return message;
    }
}
