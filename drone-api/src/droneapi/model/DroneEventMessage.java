package droneapi.model;

/**
 * Created by Cedric on 3/17/2015.
 */
public class DroneEventMessage {

    private Object innerMsg;

    public DroneEventMessage(Object innerMsg) {
        this.innerMsg = innerMsg;
    }

    public Object getInnerMsg() {
        return innerMsg;
    }

    public Class getIdentifier(){
        return innerMsg.getClass();
    }
}
