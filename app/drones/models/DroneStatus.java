package drones.models;

import scala.concurrent.Future;

/**
 * Created by Cedric on 3/8/2015.
 */
public interface DroneStatus {
    Future<FlyingState> getFlyingState();
    Future<Location> getLocation();
    Future<Byte> getBatteryPercentage();
    Future<Double> getAltitude();
    Future<Rotation> getRotation();
    Future<Speed> getSpeed();
}
