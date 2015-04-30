import droneapi.model.properties.Location;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Cedric on 4/2/2015.
 */
public class LocationTest {

    @Test
    public void testLocationGetters(){
        Location plateau = new Location(51.046274, 3.724952, 2);
        Assert.assertEquals(plateau.getLatitude(), 51.046274, 0);
        Assert.assertEquals(plateau.getLongitude(),  3.724952, 0);
        Assert.assertEquals(plateau.getHeight(), 2, 0);
    }

    @Test
    public void testLocationDistance(){

        // Distance plateau-brug = 145
        Location plateau = new Location(51.046274, 3.724952, 0);
        Location brug = new Location(51.045681, 3.726754, 0);
        double distance = Location.distance(plateau, brug);
        Assert.assertEquals(distance, 145, 5d);

        // Distance Zuiderpoort - plateau =  1,32 km
        Location zuiderpoort = new Location(51.036316, 3.735273, 0);
        distance =  Location.distance(plateau, zuiderpoort);
        Assert.assertEquals(distance, 1320, 10d);
    }

    @Test
    public void testBearing(){
        Location plateau = new Location(51.046273, 3.724918, 0);
        Location other = new Location(51.054057, 3.732049, 0);
        float bearing = Location.getBearing(plateau, other);
        Assert.assertEquals(bearing, 30f, 1f);

        Location currentLocation = new Location(51.045051, 3.730360, 0);
        Location toLocation = new Location(51.046279, 3.724921, 0);
        bearing = Location.getBearing(currentLocation, toLocation);
        Assert.assertEquals(bearing, 290f, 1f);

        // Same location
        bearing = Location.getBearing(currentLocation, currentLocation);
        Assert.assertEquals(bearing, 0f, 1f);

        // Same longitude
        currentLocation = new Location(51.045051, 3.730360, 0);
        toLocation = new Location(51.046051, 3.730360, 0);
        bearing = Location.getBearing(currentLocation, toLocation);
        Assert.assertEquals(bearing, 0f, 1f);

        // Same latitude
        currentLocation = new Location(51.045051, 3.730360, 0);
        toLocation = new Location(51.045051, 3.740360, 0);
        bearing = Location.getBearing(currentLocation, toLocation);
        Assert.assertEquals(bearing, 90f, 1f);
        // Same latitude, but locations swapped (180deg diff)
        bearing = Location.getBearing(toLocation, currentLocation);
        Assert.assertEquals(bearing, 270f, 1f);
    }
}
