import drones.models.Location;
import org.junit.Test;
import org.junit.Assert;

/**
 * Created by Cedric on 4/2/2015.
 */
public class LocationTest {

    @Test
    public void testLocationDistance(){

        // Distance plateau-brug = 145
        Location plateau = new Location(51.046274, 3.724952, 0);
        Location brug = new Location(51.045681, 3.726754, 0);
        double distance = Location.distance(plateau, brug);
        Assert.assertTrue(Math.abs(distance - 145) < 5d);

        // Distance Zuiderpoort - plateau =  1,32 km
        Location zuiderpoort = new Location(51.036316, 3.735273, 0);
        distance =  Location.distance(plateau, zuiderpoort);
        Assert.assertTrue(Math.abs(distance - 1320) < 10);
    }
}
