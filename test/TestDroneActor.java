import akka.japi.pf.UnitPFBuilder;
import drones.models.DroneActor;
import scala.concurrent.Promise;

/**
 * Created by Cedric on 4/2/2015.
 */
public class TestDroneActor extends DroneActor {

    public TestDroneActor(){

    }

    @Override
    protected void init(Promise<Void> p) {

    }

    @Override
    protected void takeOff(Promise<Void> p) {

    }

    @Override
    protected void land(Promise<Void> p) {

    }

    @Override
    protected void emergency(Promise<Void> p) {

    }

    @Override
    protected void move3d(Promise<Void> p, double vx, double vy, double vz, double vr) {

    }

    @Override
    protected void moveToLocation(Promise<Void> p, double latitude, double longitude, double altitude) {

    }

    @Override
    protected void cancelMoveToLocation(Promise<Void> p) {

    }

    @Override
    protected void setMaxHeight(Promise<Void> p, float meters) {

    }

    @Override
    protected void setMaxTilt(Promise<Void> p, float degrees) {

    }

    @Override
    protected void setOutdoor(Promise<Void> p, boolean outdoor) {

    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {

    }

    @Override
    protected void flatTrim(Promise<Void> p) {

    }

    @Override
    protected void reset(Promise<Void> p) {

    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return null;
    }
}
