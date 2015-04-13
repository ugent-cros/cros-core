package models;

import com.fasterxml.jackson.annotation.JsonRootName;
import play.data.validation.Constraints;
import utilities.ModelHelper;

import javax.persistence.Embeddable;

/**
 * Created by matthias on 19/03/2015.
 */
@JsonRootName("location")
@Embeddable
public class Location {

    @Constraints.Required
    protected double longitude;
    @Constraints.Required
    protected double latitude;
    @Constraints.Required
    protected double altitude;

    public Location() {
        this(0.0,0.0,0.0);
    }

    public Location(double longitude, double latitude, double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof Location))
            return false;
        Location checkpoint = (Location) obj;
        boolean isEqual = ModelHelper.compareFloatingPoints(this.longitude, checkpoint.longitude);
        isEqual &= ModelHelper.compareFloatingPoints(this.latitude, checkpoint.latitude);
        return isEqual && ModelHelper.compareFloatingPoints(this.altitude, checkpoint.altitude);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(altitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    // Radius of the earth in meters
    public static final int EARTH_RADIUS = 6371000;

    /**
     * Calculates the distances between two locations using the 'haversine' formula.
     * Source: http://www.movable-type.co.uk/scripts/latlong.html
     * Taking into account the latitude and longitude, not the altitude!
     *
     * @param loc1 first location
     * @param loc2 second location
     * @return the distance between two location in meters.
     */
    public static double distance(Location loc1, Location loc2) {
        double lat1 = loc1.getLatitude();
        double lat2 = loc2.getLatitude();
        double lon1 = loc1.getLongitude();
        double lon2 = loc2.getLongitude();
        // Conversion to radians for Math functions.
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        // Sin(dPhi/2)^2 + Cos(dPhi/2)^2 + Sin(dLambda/2)^2
        double c = Math.pow(Math.sin(dPhi / 2), 2)
                + Math.pow(Math.cos(dPhi / 2), 2)
                + Math.pow(Math.sin(dLambda / 2), 2);
        c = 2 * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
        // Final result in meters
        return EARTH_RADIUS * c;
    }
}

