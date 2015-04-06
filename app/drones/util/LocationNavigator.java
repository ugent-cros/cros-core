package drones.util;

import drones.commands.MoveCommand;
import drones.models.Location;

/**
 * Created by Cedric on 4/5/2015.
 */
public class LocationNavigator {
    private Location previousLocation;
    private Location goal;
    private float maxAngularVelocity;
    private float maxForwardVelocity;
    private float maxVerticalVelocity;
    private float gpsAccuracy;
    private boolean hadHeading;

    private static final float MAX_DIFF_BEARING = 30f;
    private static final double MIN_HEIGHT_DIFF = 0.5;
    private static final double MIN_BEARING_DIFF = 10f;
    private static final double MIN_VR_VALUE = 0.1;

    /**
     * Creates a new location navigation session
     * @param goal The goal location
     * @param currentLocation The current location
     * @param gpsAccuracy The accuracy of the GPS in meters
     * @param maxAngularVelocity The maximum angular velocity in degrees (for vz = 1)
     * @param maxForwardVelocity The maximum forward velocity in m/s
     * @param maxVerticalVelocity The maximum vertical velocity (up) in m/s
     */
    public LocationNavigator(Location currentLocation, Location goal, float gpsAccuracy, float maxAngularVelocity, float maxForwardVelocity, float maxVerticalVelocity){
        this.goal = goal;
        this.previousLocation = currentLocation;
        this.gpsAccuracy = gpsAccuracy;
        this.maxAngularVelocity = maxAngularVelocity;
        this.maxForwardVelocity = maxForwardVelocity;
        this.maxVerticalVelocity = maxVerticalVelocity;
    }

    public MoveCommand update(Location location){
        float[] res = Location.computeDistanceAndBearing(previousLocation, location); // calculate the currently moved direction
        float movedDistance = res[0];
        float movedBearing = res[1];

        double vr = 0;
        double heightDiff = goal.getHeight() - location.getHeight();
        if(Math.abs(heightDiff) > MIN_HEIGHT_DIFF){ // check if we have to go up/down
            vr = Math.abs(heightDiff) > maxVerticalVelocity ? Math.signum(heightDiff) : (heightDiff / maxVerticalVelocity); // pos = rise, neg = down
        }

        res = Location.computeDistanceAndBearing(location, goal);
        float goalDistance = res[0];
        float goalBearing = res[1];

        double vx = goalDistance > maxForwardVelocity ? 1d : (goalDistance / maxForwardVelocity); // move max forward

        if(movedDistance > gpsAccuracy){
            previousLocation = location; // significant location update

            if(goalDistance < gpsAccuracy){ // we arrived at our destination with best effort accuracy
                if(vr != 0){
                    return new MoveCommand(0, 0, 0, vr);
                } else {
                    return null; // Arrived
                }
            } else {
                hadHeading = true;
                float bearingDiff = goalBearing - movedBearing; // calculate difference angle that we have to correct
                if(Math.abs(bearingDiff) < MIN_BEARING_DIFF){ // we don't care about 10 degrees off, continue
                    return new MoveCommand(vx, 0, 0, vr);
                } else {
                    if(bearingDiff > 180f){
                        bearingDiff -= 360f; // faster to go left
                    } else if(bearingDiff < -180f) {
                        bearingDiff += 360f; // faster to go right
                    }
                    double vz = Math.abs(bearingDiff) > maxAngularVelocity ? Math.signum(bearingDiff) : (bearingDiff / maxAngularVelocity); // take max angle or relative to angular velocity
                    if(bearingDiff > MAX_DIFF_BEARING){
                        vx *= 0.5; // When we require a high angle we lower the forward speed
                    }
                    return new MoveCommand(vx, 0, vz, vr);
                }
            }
        } else {
            if(goalDistance < gpsAccuracy && Math.abs(vr) < MIN_VR_VALUE){ // we started in region we wanted already
                return null;
            } else {
                if(!hadHeading) { // when no angle update has been sent, discover using slower movement for faster GPS updates
                    vx *= 0.5;
                }
                return new MoveCommand(vx, 0, 0, vr); // movement not significant enough to take angle measurement into account
            }

        }
    }

    public Location getGoal() {
        return goal;
    }
}
