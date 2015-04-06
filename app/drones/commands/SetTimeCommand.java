package drones.commands;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * Created by Cedric on 4/6/2015.
 */
public class SetTimeCommand implements Serializable {
    private DateTime time;

    public SetTimeCommand(DateTime time) {
        this.time = time;
    }

    public DateTime getTime() {
        return time;
    }
}
