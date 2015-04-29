package messages;

import java.io.Serializable;

/**
 * Created by Cedric on 3/13/2015.
 */
public class MoveRequestMessage implements Serializable {
    private double vx;
    double vy;
    double vz;
    double vr;

    public MoveRequestMessage(double vx, double vy, double vz, double vr) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.vr = vr;
    }

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
    }

    public double getVz() {
        return vz;
    }

    public double getVr() {
        return vr;
    }
}
