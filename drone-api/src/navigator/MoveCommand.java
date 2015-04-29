package navigator;

import java.io.Serializable;

/**
 * Created by Cedric on 3/13/2015.
 */
public class MoveCommand implements Serializable {
    double vx, vy, vz, vr;

    public MoveCommand(double vx, double vy, double vz, double vr) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoveCommand that = (MoveCommand) o;

        if (Double.compare(that.vx, vx) != 0) return false;
        if (Double.compare(that.vy, vy) != 0) return false;
        if (Double.compare(that.vz, vz) != 0) return false;
        return Double.compare(that.vr, vr) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(vx);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(vy);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(vz);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(vr);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "MoveCommand{" +
                "vx=" + vx +
                ", vy=" + vy +
                ", vz=" + vz +
                ", vr=" + vr +
                '}';
    }
}
