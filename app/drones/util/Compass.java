package drones.util;

import drones.models.Location;

/**
 * Created by Cedric on 4/2/2015.
 */
public class Compass {

    /**
     * Corrects the compass reading with current declination
     * @param compassRadians Radians of the reading
     * @param readFrom Location where compass is read
     * @return Corrected compass reading
     */
    public static double calculateHeading(double compassRadians, Location readFrom){
        GeomagneticField field = new GeomagneticField((float)readFrom.getLatitude(), (float)readFrom.getLongitude(), (float)readFrom.getHeigth(), System.currentTimeMillis());
        float declination = field.getDeclination();
        return compassRadians + Math.toRadians(declination);
    }
}
