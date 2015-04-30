package droneapi.model.properties;

import java.io.Serializable;

/**
 * Created by Cedric on 3/10/2015.
 */
public class Speed implements Serializable {
    private double vx;
    private double vy;
    private double vz;

    public Speed(double vx, double vy, double vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
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
}
