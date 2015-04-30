package droneapi.model;

import java.io.Serializable;

/**
 * Created by Cedric on 3/8/2015.
 */
public class DroneException extends RuntimeException implements Serializable{
    public DroneException(String cause){
        super(cause);
    }
}
