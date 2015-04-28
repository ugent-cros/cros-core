package messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/22/2015.
 */
public class SetHullRequestMessage implements Serializable {
    boolean hull;

    public SetHullRequestMessage(boolean hull) {
        this.hull = hull;
    }

    public boolean hasHull() {
        return hull;
    }
}
