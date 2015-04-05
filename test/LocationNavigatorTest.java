import drones.commands.MoveCommand;
import drones.models.Location;
import drones.util.LocationNavigator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Cedric on 4/5/2015.
 */
public class LocationNavigatorTest {

    @Test
    public void should_give_null_when_arrived(){
        Location start = new Location(51.046266, 3.724902, 1);
        Location goal = new Location(51.046267, 3.724903, 1);
        LocationNavigator nav = new LocationNavigator(start, goal, 2f, 60f, 4f, 1f);
        Assert.assertNull(nav.update(new Location(start.getLatitude(), start.getLongtitude(), start.getHeigth() + 0.4)));
    }

    @Test
    public void should_still_go_up_when_close(){
        Location start = new Location(51.046266, 3.724902, 1);
        Location goal = new Location(51.046266112, 3.724902112, 3);
        LocationNavigator nav = new LocationNavigator(start, goal, 2f, 60f, 4f, 1f);
        MoveCommand cmd1 = nav.update(new Location(start.getLatitude(), start.getLongtitude(), start.getHeigth() + 0.4));
        MoveCommand cmd2 = nav.update(new Location(start.getLatitude(), start.getLongtitude(), start.getHeigth() + 1.4));
        MoveCommand cmd3 = nav.update(new Location(start.getLatitude(), start.getLongtitude(), start.getHeigth() + 2.4));
        Assert.assertNotNull(cmd1);
        Assert.assertTrue(cmd1.getVr() == 1);
        Assert.assertNotNull(cmd2);
        Assert.assertTrue(cmd2.getVr() > 0);
        Assert.assertNull(cmd3); // no third command required, arrived
        Assert.assertEquals(cmd1.getVx(), 0, 0.1);
        Assert.assertEquals(cmd2.getVx(), 0, 0.1);
    }

    @Test
    public void should_correct_angle(){
        Location start = new Location(51.046266, 3.724902, 1);
        Location goal = new Location(51.046253, 3.725443, 3); // goal east of start
        Location firstStep = new Location(51.046366, 3.724877, 2); // first step north of start
        LocationNavigator nav = new LocationNavigator(start, goal, 2f, 60f, 4f, 1f);
        LocationNavigator nav2 = new LocationNavigator(start, goal, 2f, 120f, 4f, 1f);
        MoveCommand cmda1 = nav.update(firstStep);
        Assert.assertEquals(cmda1.getVz(), 1, 0); // attempt to turn right, full power (>90 degrees)
        MoveCommand cmdb1 = nav2.update(firstStep);
        Assert.assertTrue(cmdb1.getVz() < 1); // angular velocity = 120, correction angle = +-116

        Location secondStep = new Location(51.046318, 3.725011, 2.9);
        MoveCommand cmda2 = nav.update(secondStep);
        Assert.assertTrue(cmda2.getVz() < 0); // overshoot, correct to the left

        // Trajectory
        nav.update(new Location(51.046259, 3.725234, 2.95));
        nav.update(new Location(51.046252, 3.725354, 3.02));
        Assert.assertNull(nav.update(new Location(51.04625322, 3.72544322, 2.96)));
    }

    @Test
    public void should_prefer_left_turn(){
        Location start = new Location(51.046266, 3.724902, 1);
        Location goal = new Location(51.046167, 3.724808, 2.5); // southwest of start
        Location firstStep = new Location(51.046366, 3.724877, 2); // first step north of start
        LocationNavigator nav = new LocationNavigator(start, goal, 2f, 60f, 4f, 1f);
        MoveCommand cmd1 = nav.update(firstStep);
        Assert.assertTrue(cmd1.getVz() < 0); // prefer left turn instead of long right turn to go to southwest
    }
}
