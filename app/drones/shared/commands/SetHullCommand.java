package drones.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class SetHullCommand implements Serializable {
    private boolean hull;

    public SetHullCommand(boolean hull) {
        this.hull = hull;
    }

    public boolean hasHull() {
        return hull;
    }
}
