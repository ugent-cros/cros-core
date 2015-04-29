package utilities.frontendSimulator;

import messages.*;
import drones.messages.*;
import drones.models.scheduler.messages.from.AssignmentCompletedMessage;
import drones.models.scheduler.messages.from.AssignmentStartedMessage;
import drones.models.scheduler.messages.from.DroneAssignedMessage;
import models.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Benjamin on 14/04/2015.
 */
public class AssignmentSimulator implements Runnable {
    private Assignment assignment;
    private Drone drone;
    private double step = 0.00005;
    private boolean run = true;
    private NotificationSimulator notificationSimulator;
    private Basestation basestation;

    public AssignmentSimulator(Assignment assignment, Drone drone, NotificationSimulator notificationSimulator) {
        this.assignment = assignment;
        this.drone = drone;
        this.notificationSimulator = notificationSimulator;
        basestation = Basestation.FIND.all().get(0);
    }

    @Override
    public void run() {
        try {
            assignment = initAssigment(assignment);
            notificationSimulator.sendMessage("droneAssigned", assignment.getId(),
                    new DroneAssignedMessage(assignment.getId(),drone.getId()));
            Thread.sleep(2000);
            int current = 0;
            Checkpoint currentCheckpoint = assignment.getRoute().get(current);
            Location currentLocation = currentCheckpoint.getLocation();
            notificationSimulator.sendMessage("locationChanged", drone.getId(),
                    new LocationChangedMessage(currentLocation.getLongitude(),
                            currentLocation.getLatitude(),
                            currentLocation.getAltitude()));
            notificationSimulator.sendMessage("assignmentStarted", assignment.getId(),
                    new AssignmentStartedMessage(assignment.getId()));
            while (run) {
                // Wait seconds
                try {
                    Thread.sleep(currentCheckpoint.getWaitingTime() * 1000);
                } catch (InterruptedException e) {
                    // Continue loop
                }
                // Move to next location
                Checkpoint nextCheckpoint = assignment.getRoute().get(current + 1);
                Location nextLocation = nextCheckpoint.getLocation();
                simulateMoveToNextLocation(currentLocation, nextLocation);

                // Next checkpoint reached
                currentCheckpoint = nextCheckpoint;
                currentLocation = nextLocation;
                simulateNextLocationReached(currentLocation);
                current++;
            }
        } catch(Exception ex) {
            // An error occured in the assignments thread, most likely due to initDB during execution.
            // Try to make drone available again if possible
            run = false;
            try {
                drone.setStatus(Drone.Status.AVAILABLE);
                drone.update();
            } catch(Exception e) {
                // Drone no longer available after initDB
            }
        }
    }

    public Assignment initAssigment(Assignment assignment) {
        // Prepend the default basestation
        Location location = basestation.getLocation();
        assignment.getRoute().add(0, new Checkpoint(
            location.getLongitude(), location.getLatitude(), location.getAltitude()));
        List<Checkpoint> route = assignment.getRoute().stream().map(c -> new Checkpoint(c.getLocation().getLongitude(),
                c.getLocation().getLatitude(), c.getLocation().getAltitude(), c.getWaitingTime())).collect(Collectors.toList());
        Assignment extendedAssignment = new Assignment(route, assignment.getCreator());
        extendedAssignment.setAssignedDrone(assignment.getAssignedDrone());
        extendedAssignment.setProgress(1);
        extendedAssignment.save();
        assignment.delete();
        return extendedAssignment;
    }

    public void simulateMoveToNextLocation(Location current, Location next) {
        // Step towards the next checkpoint
        double difLong = next.getLongitude() - current.getLongitude();
        double difLat = next.getLatitude() - current.getLatitude();
        double absDifLong = Math.abs(difLong);
        double absDifLat = Math.abs(difLat);
        double stepLong = step;
        double stepLat = step;
        while((absDifLong - stepLong >= step || absDifLat - stepLat >= step) && run) {
            notificationSimulator.sendMessage("locationChanged", drone.getId(),
                    new LocationChangedMessage(current.getLongitude() + stepLong * (difLong < 0 ? -1 : 1),
                            current.getLatitude() + stepLat * (difLat < 0 ? -1 : 1),
                            current.getAltitude()));
            stepLong += absDifLong - stepLong < step ? 0 : step;
            stepLat += absDifLat - stepLat < step ? 0 : step;
            try {
                Thread.sleep(250);
            } catch(InterruptedException ex) {
                // continue the loop
            }
        }
    }

    public void simulateNextLocationReached(Location current) {
        notificationSimulator.sendMessage("locationChanged", drone.getId(),
                new LocationChangedMessage(current.getLongitude(),
                        current.getLatitude(),
                        current.getAltitude()));
        // Possibly end reached
        assignment.setProgress(assignment.getProgress() + 1);
        assignment.update();

        if(assignment.getProgress() == assignment.getRoute().size() && run) {
            notificationSimulator.sendMessage("assignmentCompleted", assignment.getId(),
                    new AssignmentCompletedMessage(assignment.getId()));
            drone.setStatus(Drone.Status.AVAILABLE);
            drone.update();
            run = false;
        }
        else {
            notificationSimulator.sendMessage("assignmentProgressChanged", assignment.getId(),
                    new AssignmentProgressChangedMessage(assignment.getProgress()));
        }
    }
}
