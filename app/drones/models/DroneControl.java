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

    /**
     *
     * @return Promise whether the emergency was initiated
     */
    //Future<Void> emergency(); // @TODO

    /***
     * Moves the drone in a 3D plane
     * @param vx Pitch left/right between [-1;1]
     * @param vy Pitch forward/backward between [-1;1]
     * @param vz Up/down between [-1;1]
     * @param vr Rotate left/right between [-1;1]
     * @return Promise whether request was initiated (but not yet completed)
     */
    Future<Void> move3d(double vx, double vy, double vz, double vr);

    /***
     * Moves the drone in a 2D plane
     * @param vx Pitch left/right between [-1;1]
     * @param vy Pitch forward/backward between [-1;1]
     * @param vr Rotate left/right between [-1;1]
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

    /**
     * Calibrates the drone when on the ground
     * @return Promise whether the request was initiated
     */
    Future<Void> calibrate(boolean outdoor, boolean hull);

    /**
     * Calibrates the drone when on ground with current settings
     * @return Promise whether the request was initiated
     */
    Future<Void> flatTrim();

    /**
     * Sets the drone's outdoor status
     * @param outdoor True when outdoor
     * @return Promise whether the request was initiated
     */
    Future<Void> setOutdoor(boolean outdoor);

    /**
     * Sets the drone's hull status (protection)
     * @param hull True when attached
     * @return Promise whether the request was initiated
     */
    Future<Void> setHull(boolean hull);

    /**
     *
     * @return An image taken by a camera of the drone
     */
    Future<byte[]> getImage();

    /**
     * Performs a flip in the air
     * @param type Direction of the flip
     * @return Promise whether the request was initiated
     */
    Future<Void> flip(FlipType type);

    /**
     *
     * @return Promise whether the request was initiated
     */
    Future<Void> startVideo();

    /**
     * Stops the drone communication
     */
    void stop();
}
