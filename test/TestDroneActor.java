import akka.japi.pf.UnitPFBuilder;
import droneapi.model.DroneActor;
import droneapi.model.DroneException;
import droneapi.model.properties.FlipType;
import scala.concurrent.Promise;

/**
 * Created by Cedric on 4/2/2015.
 */
public class TestDroneActor extends DroneActor {

    public TestDroneActor(){

    }

    @Override
    protected void stop() {

    }

    @Override
    protected void init(Promise<Void> p) {
        p.success(null);
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
        p.success(null);
    }

    @Override
    protected void setHull(Promise<Void> p, boolean hull) {
        p.success(null);
    }

    @Override
    protected void flatTrim(Promise<Void> p) {
        p.success(null);
    }

    @Override
    protected void reset(Promise<Void> p) {
        p.failure(new DroneException("Not implemented yet."));
    }

    @Override
    protected void flip(Promise<Void> p, FlipType type) {
        p.failure(new DroneException("Not implemented yet."));
    }

    @Override
    protected void initVideo(Promise<Void> p) {
        p.failure(new DroneException("Not implemented yet."));
    }

    @Override
    protected void stopVideo(Promise<Void> p) {
        p.failure(new DroneException("Not implemented yet."));
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return null;
    }
}
