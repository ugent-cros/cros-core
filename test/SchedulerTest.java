import com.avaje.ebean.Ebean;
import drones.models.DroneException;
import drones.models.scheduler.AssignmentMessage;
import drones.models.scheduler.Scheduler;
import drones.models.scheduler.SchedulerException;
import drones.models.scheduler.SimpleScheduler;
import models.Assignment;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ronald on 6/04/2015.
 */
public class SchedulerTest extends TestSuperclass {

    @Before
    public void setup() {
        startFakeApplication();
    }

    @After
    public void tearDown() {
        stopFakeApplication();
    }

    @Test
    public void getScheduler_Started_Succeeds() throws Exception{
        Scheduler.getScheduler();
    }

    @Test
    public void stop_Started_Succeeds() throws Exception{
        Scheduler.stop();
    }

    @Test(expected = SchedulerException.class)
    public void start_Started_Fails() throws Exception{
        Scheduler.start(SimpleScheduler.class);
    }

    @Test
    public void start_NotStarted_Succeeds() throws Exception{
        Scheduler.stop();
        Scheduler.start(SimpleScheduler.class);
    }

    @Test(expected = SchedulerException.class)
    public void getScheduler_NotStarted_Fails() throws Exception{
        Scheduler.stop();
        Scheduler.getScheduler();
    }

    @Test(expected = SchedulerException.class)
    public void stop_NotStarted_Fails() throws Exception{
        Scheduler.stop();
        Scheduler.stop();
    }


    @Test
    public void submitAssignments_Succeeds() throws Exception{
        List<Assignment> assignments = new ArrayList<>();
        for(long i = 1; i <= 10; i++){
            Assignment assignment = new Assignment();
            assignment.setId(i);
            assignments.add(assignment);
        }
        Ebean.save(assignments);
        Scheduler.getScheduler().tell(new AssignmentMessage(1),null);
    }
}
