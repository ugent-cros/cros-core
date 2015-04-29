package navigator;

import model.properties.Location;

/**
 * Created by Cedric on 4/5/2015.
 */
public class LocationNavigator {
    private Location previousLocation;
    private Location goal;
    private float maxAngularVelocity;
    private float maxVerticalVelocity;
    private float gpsAccuracy;
    private boolean hadHeading;

    // Rotational values
    private float degreesLeft = 0;
    private boolean left;

    private static final float MAX_DIFF_BEARING = 30f;
    private static final double MIN_HEIGHT_DIFF = 0.3;
    private static final double MIN_BEARING_DIFF = 10f;
    private static final double MIN_VR_VALUE = 0.1;
    private static final double SLOW_RADIUS = 10; //go slower within 10m

    /**
     * Creates a new location navigation session
     * @param goal The goal location
     * @param currentLocation The current location
     * @param gpsAccuracy The accuracy of the GPS in meters
     * @param maxAngularVelocity The maximum angular velocity in degrees (for vz = 1)
     * @param maxVerticalVelocity The maximum vertical velocity (up) in m/s
     */
    public LocationNavigator(Location currentLocation, Location goal, float gpsAccuracy, float maxAngularVelocity, float maxVerticalVelocity){
        this.goal = goal;
        this.previousLocation = currentLocation;
        this.gpsAccuracy = gpsAccuracy;
        this.maxAngularVelocity = maxAngularVelocity;
        this.maxVerticalVelocity = maxVerticalVelocity;
    }

    public MoveCommand update(Location location){
        if(goal == null || location == null) {
            return null;
        }

        float[] res = Location.computeDistanceAndBearing(previousLocation, location); // calculate the currently moved direction
        float movedDistance = res[0];
        float movedBearing = res[1];

        double vz = 0;
        double heightDiff = goal.getHeight() - location.getHeight();
        if(Math.abs(heightDiff) > MIN_HEIGHT_DIFF){ // check if we have to go up/down
            vz = Math.abs(heightDiff) > maxVerticalVelocity ? Math.signum(heightDiff) : (heightDiff / maxVerticalVelocity); // pos = rise, neg = down
        }

        res = Location.computeDistanceAndBearing(location, goal);
        float goalDistance = res[0];
        float goalBearing = res[1];

        // When within 10m, go slower
        double vx;
        if(goalDistance < SLOW_RADIUS && goalDistance > gpsAccuracy){
            vx = 0.5;
        } else if(goalDistance < gpsAccuracy) {
            vx = goalDistance / gpsAccuracy; // the closer, the slower
        } else {
            vx = 1d; // else full ahead
        }

        if(movedDistance > gpsAccuracy){
            previousLocation = location; // significant location update

            if(goalDistance < gpsAccuracy*1.2){ // we arrived at our destination with best effort accuracy
                if(vz != 0){
                    return new MoveCommand(0, 0, vz, 0);
                } else {
                    return null; // Arrived
                }
            } else {
                hadHeading = true;
                float bearingDiff = goalBearing - movedBearing; // calculate difference angle that we have to correct
                if(Math.abs(bearingDiff) < MIN_BEARING_DIFF){ // we don't care about 10 degrees off, continue
                    return new MoveCommand(vx, 0, vz, 0);
                } else {

                    if(bearingDiff > 180f){
                        bearingDiff -= 360f; // faster to go left
                    } else if(bearingDiff < -180f) {
                        bearingDiff += 360f; // faster to go right
                    }

                    double vr = 0;
                    if(Math.abs(bearingDiff) > MAX_DIFF_BEARING/2){
                        vx = 0;
                        left = bearingDiff < 0;

                        float todoTurn = Math.abs(bearingDiff) > maxAngularVelocity ? maxAngularVelocity : Math.abs(bearingDiff); // degrees that have to be turned or max capacity
                        if(maxAngularVelocity > todoTurn)
                            vr = (todoTurn / maxAngularVelocity) * (left ? -1 : 1); // normalize the rotation
                        else
                            vr = left ? -1 : 1; // full power rotate

                        degreesLeft = Math.max(0, Math.abs(bearingDiff) - todoTurn);
                    }

                    return new MoveCommand(vx, 0, vz, vr);
                }
            }
        } else {
            if(goalDistance < gpsAccuracy*1.2 && Math.abs(vz) < MIN_VR_VALUE){ // we started in region we wanted already
                return null;
            } else {
                if(!hadHeading) { // when no angle update has been sent, discover using slower movement for faster GPS updates
                    vx *= 0.5;
                }

                double vr = 0;
                if(degreesLeft > 0) {
                    vx = 0; // don't move forward while rotating
                    float todoTurn = Math.abs(degreesLeft) > maxAngularVelocity ? maxAngularVelocity : Math.abs(degreesLeft); // degrees that have to be turned or max capacity
                    if (maxAngularVelocity > todoTurn)
                        vr = (todoTurn / maxAngularVelocity) * (left ? -1 : 1); //normalize the rotation
                    else
                        vr = left ? -1 : 1; //full power rotate

                    degreesLeft = Math.max(0, degreesLeft - todoTurn); // subtract the already turned rotation
                }
                return new MoveCommand(vx, 0, vz, vr); // movement not significant enough to take angle measurement into account
            }
        }
    }

    public Location getGoal() {
        return goal;
    }

    public void setGoal(Location goal) {
        this.goal = goal;
        this.degreesLeft = 0;
    }

    public Location getCurrentLocation() {
        return previousLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.previousLocation = currentLocation;
    }
}
