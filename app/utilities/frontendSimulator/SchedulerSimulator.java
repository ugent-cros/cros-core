package utilities.frontendSimulator;

import models.Assignment;
import models.Drone;
import play.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Benjamin on 14/04/2015.
 */
public class SchedulerSimulator implements Runnable {
    private boolean run = true;
    private NotificationSimulator notificationSimulator;
    private ExecutorService pool = null;
    private Drone availableDrone = null;
    private int counter = 0;

    public SchedulerSimulator(NotificationSimulator notificationSimulator) {
        this.notificationSimulator = notificationSimulator;
    }

    @Override
    public void run() {
        pool = Executors.newFixedThreadPool(10);
        try {
            availableDrone = Drone.FIND.where().eq("status", Drone.Status.AVAILABLE).findList().get(0);
            while (run) {
                List<Assignment> found = Assignment.FIND.where().eq("progress", 0).findList();
                while (found.isEmpty() && run) {
                    found = Assignment.FIND.where().eq("progress", 0).findList();
                }
                int amount = Assignment.FIND.findRowCount();
                if(found.size() + counter > amount)
                    throw new RuntimeException("InitDB detected");
                Thread.sleep(5000);
                for(int i = 0; i < found.size() && run; ++i) {
                    counter++;
                    Assignment assignment = found.get(i);
                    while (availableDrone == null && run) {
                        try {
                            Thread.sleep(2000);
                            availableDrone = Drone.FIND.where().eq("status", Drone.Status.AVAILABLE).findList().get(0);
                        } catch (InterruptedException e) {
                            Logger.error(e.getMessage(), e);
                        }
                    }
                    if(run) {
                        // Update drone
                        availableDrone.setStatus(Drone.Status.UNAVAILABLE);
                        availableDrone.update();
                        // Update assignment
                        assignment.setAssignedDrone(availableDrone);
                        assignment.setProgress(1);
                        assignment.update();
                        pool.execute((new AssignmentSimulator(assignment, availableDrone, notificationSimulator)));
                        availableDrone = null;
                    }
                }
            }
        } catch(Exception ex) {
            // An error occured in the sheduler thread, most likely due to initDB during execution.
            pool.shutdownNow();
            run = false;
            if(availableDrone != null) {
                try {
                    availableDrone.setStatus(Drone.Status.AVAILABLE);
                    availableDrone.update();
                } catch(Exception e) {
                    // Attempt to reset drone failed
                }
            }
        }
    }
}
