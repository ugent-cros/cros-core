package drones.shared.commands;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * Created by Cedric on 4/6/2015.
 */
public class SetDateCommand implements Serializable {
    private DateTime date;

    public SetDateCommand(DateTime date) {
        this.date = date;
    }

    public DateTime getDate() {
        return date;
    }
}
