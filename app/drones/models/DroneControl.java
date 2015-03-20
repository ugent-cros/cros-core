package drones.models;

import scala.concurrent.Future;

/**
 * Created by Cedric on 3/8/2015.
 */
public interface DroneControl {
    /**
     * Initiates the drone connection
     * @return Promise whether the init was successfull
     */
    Future<Void> init();

    /**
     * Initiates a takeoff action
     * @return Promise whether the takeoff was initiated
     */
    Future<Void> takeOff();

    /**
     * Initiates a landing action
     * @return Promise whether the landing was initiated
     */
    Future<Void> land();

    /***
     * Moves the drone in a 3D plane
     * @param vx X velocity [m/s]
     * @param vy Y velocity [m/s]
     * @param vz Z velocity [m/s]
     * @param vr Angular velocity [rad/s]
     * @return Promise whether request was initiated (but not yet completed)
     */
    Future<Void> move3d(double vx, double vy, double vz, double vr);

    /***
     * Moves the drone in a 2D plane
     * @param vx X velocity [m/s]
     * @param vy Y velocity [m/s]
     * @param vr Angular velocity [rad/s]
     * @return Promise whether request was initiated (but not yet completed)
     */
    Future<Void> move(double vx, double vy, double vr);

    /**
     * Sets the maximum height for the drone
     * @param meters The height
     * @return Promise whether the request was initiated
     */
    Future<Void> setMaxHeight(float meters);

    /**
     * Sets the maximum drone tilt
     * @param degrees Maximum angle in degrees
     * @return Promise whether the request was initiated
     */
    Future<Void> setMaxTilt(float degrees);

    /**
     * Requests the drone to move to GPS coordinates
     * @param latitude The latitude in decimal format
     * @param longitude The longitude in decimal format
     * @param altitude The altitude in meters
     * @return Promise whether the request was initiated
     */
    Future<Void> moveToLocation(double latitude, double longitude, double altitude);

    /**
     * Cancels the drone when moving to a GPS location
     * @return Promise whether the request was initiated
     */
    Future<Void> cancelMoveToLocation();

}
