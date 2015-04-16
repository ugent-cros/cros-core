package utilities.frontendSimulator;

import drones.messages.DroneAssignedMessage;
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
    private static final ExecutorService pool =     Executors.newFixedThreadPool(10);

    public SchedulerSimulator(NotificationSimulator notificationSimulator) {
        this.notificationSimulator = notificationSimulator;

    }

    @Override
    public void run() {
        try {
            Drone availableDrone = Drone.FIND.where().eq("status", Drone.Status.AVAILABLE).findList().get(0);
            while (run) {
                List<Assignment> found = Assignment.FIND.where().eq("progress", -1).findList();
                while (found.isEmpty() && run) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Logger.error(e.getMessage(), e);
                    }
                    found = Assignment.FIND.where().eq("progress", -1).findList();
                }
                for(int i = 0; i < found.size() && run; ++i) {
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
                        assignment.setProgress(0);
                        assignment.update();
                        notificationSimulator.sendMessage("droneAssigned", assignment.getId(),
                                new DroneAssignedMessage(availableDrone.getId()));
                        pool.execute((new AssignmentSimulator(assignment, availableDrone, notificationSimulator)));
                        availableDrone = null;
                    }
                }
            }
        } catch(Exception ex) {
            Logger.error("An error occured in the sheduler thread, most likely due to initDB during execution.", ex);
            run = false;
            pool.shutdownNow();
        }
    }
}
