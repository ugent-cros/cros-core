package drones.models.scheduler;

import models.Assignment;
import models.Basestation;
import models.Checkpoint;
import models.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ronald on 19/04/2015.
 */
public class Helper {

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
        // Sin(dPhi/2)^2 + Cos(phi1) * Cos(phi2) * Sin(dLambda/2)^2
        double a = Math.pow(Math.sin(dPhi / 2), 2);
        double b = Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(dLambda / 2), 2);
        double c = a + b;
        c = 2 * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
        // Final result in meters
        return EARTH_RADIUS * c;
    }

    /**
     * Find the closest base station to a certain location
     * @param location
     * @return the closest base station
     */
    public static Basestation closestBaseStation(Location location) {
        List<Basestation> stations = Basestation.FIND.all();
        double minDist = Double.MAX_VALUE;
        Basestation closest = null;
        for (Basestation station : stations) {
            double dist = distance(station.getLocation(), location);
            if (dist < minDist) {
                minDist = dist;
                closest = station;
            }
        }
        return closest;
    }

    /**
     * Calculate the route length of an assignment.
     * @param assignment
     * @return route length or NaN if invalid route
     */
    public static double getRouteLength(Assignment assignment){
        List<Checkpoint> route = assignment.getRoute();
        if(route == null || route.isEmpty()){
            return Double.NaN;
        }
        double length = 0;
        Location from = route.get(0).getLocation();
        for(int c = 1; c < route.size(); c++){
            Location to = route.get(c).getLocation();
            length += distance(from,to);
            from = to;
        }
        return length;
    }

    /**
     * Create a direct route to a location in the form of a checkpoint list.
     * @param location
     * @return Checkpoint list route
     */
    public static List<Checkpoint> routeTo(Location location) {
        List<Checkpoint> route = new ArrayList<>();
        // Create new checkpoint with longitude, latitude, altitude
        route.add(new Checkpoint(location));
        return route;
    }

    /**
     * Convert an entity location to a drone location.
     * @param location
     * @return a drone location
     */
    public static model.properties.Location entityToDroneLocation(Location location){
        return new model.properties.Location(location.getLatitude(),location.getLongitude(),location.getAltitude());
    }

    /**
     * Convert a drone location to an entity location.
     * @param location
     * @return an entity location
     */
    public static Location droneToEntityLocation(model.properties.Location location){
        return new Location(location.getLatitude(),location.getLongitude(),location.getHeight());
    }
}
