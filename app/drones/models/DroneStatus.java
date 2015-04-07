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
    Future<DroneVersion> getVersion();
    Future<NavigationState> getNavigationState();
    Future<NavigationStateReason> getNavigationStateReason();
    Future<Boolean> isGPSFixed();
    Future<Boolean> isOnline();
    Future<Boolean> isCalibrationRequired();
}
