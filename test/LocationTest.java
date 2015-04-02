import drones.models.Location;
import org.junit.Test;
import org.junit.Assert;

/**
 * Created by Cedric on 4/2/2015.
 */
public class LocationTest {

    @Test
    public void testLocationGetters(){
        Location plateau = new Location(51.046274, 3.724952, 2);
        Assert.assertEquals(plateau.getLatitude(), 51.046274, 0);
        Assert.assertEquals(plateau.getLongtitude(),  3.724952, 0);
        Assert.assertEquals(plateau.getHeigth(), 2, 0);
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
}
