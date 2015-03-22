package drones.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class NavigateHomeCommand implements Serializable {
    boolean start;

    /**
     * Creates a navigate home command
     * @param start True for start, false for stop
     */
    public NavigateHomeCommand(boolean start) {
        this.start = start;
    }

    public boolean isStart() {
        return start;
    }
}
