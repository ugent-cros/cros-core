package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.pf.UnitPFBuilder;
import drones.models.Location;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.DroneArrivalMessage;
import drones.models.scheduler.FlightControlExceptionMessage;
import models.Drone;

import java.util.List;

/**
 * Simple Control Tower
 * DO NOT ADD A DRONE WITHIN THE NO FLY RANGE OF THE LOCATION WHERE ANOTHER DRONE WANTS TO LAND/TAKE OFF
 *
 * Created by Sander on 08/04/2015.
 */
public class SimpleControlTower extends ControlTower{

    //range between the drone can fly
    private double maxAltitude;
    private double minAltitude;
    private int maxNumberOfDrones;
    private boolean[] usedAltitudes;

    //array of drones
    private Long[] drones;
    private ActorRef[] pilots;
    private int numberOfDrones = 0;
    private List<Location> noFlyPoints;

    private boolean started = false;

    public SimpleControlTower(ActorRef reporterRef, double maxAltitude, double minAltitude, int maxNumberOfDrones) {
        super(reporterRef);
        this.maxAltitude = maxAltitude;
        this.minAltitude = minAltitude;
        this.maxNumberOfDrones = maxNumberOfDrones;
        usedAltitudes = new boolean[maxNumberOfDrones];
        drones = new Long[maxNumberOfDrones];
        pilots = new ActorRef[maxNumberOfDrones];
    }

    private double getAltitudeForIndex(int i){
        return minAltitude + i * (maxAltitude - minAltitude)/maxNumberOfDrones + (maxAltitude - minAltitude)/(2*maxNumberOfDrones);
    }

    @Override
    protected void droneArrivalMessage(DroneArrivalMessage m) {
        reporterRef.tell(m, self());

        //remove SimplePilot
    }

    @Override
    protected void addDroneMessage(AddDroneMessage m) {
        if(numberOfDrones >= maxNumberOfDrones){
            reporterRef.tell(new ControlTowerFullMessage(m),self());
        } else {
            //find available drone height
            for (int i = 0; i < maxNumberOfDrones; i++) {
                if(!usedAltitudes[i]){
                    numberOfDrones++;
                    drones[i] = m.getDroneId();
                    usedAltitudes[i] = true;
                    final double altitude = getAltitudeForIndex(i);
                    pilots[i] = getContext().actorOf(
                            Props.create(SimplePilot.class,
                                    () -> new SimplePilot(self(), m.getDroneId(), true, m.getWaypoints(), altitude)));

                    if(started){
                        //to do add needed messages
                        pilots[i].tell(new StartFlightControlMessage(), self());
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        started = true;

        for (int i = 0; i < maxNumberOfDrones; i++) {
            if(usedAltitudes[i]){
                pilots[i].tell(new StartFlightControlMessage(), self());
            }
        }
    }

    @Override
    protected void requestMessage(RequestMessage m) {
        noFlyPoints.add(m.getLocation());
        for (int i = 0; i < maxNumberOfDrones; i++) {
            if(usedAltitudes[i]){
                if(getAltitudeForIndex(i) < m.getLocation().getHeigth() && m.getRequester() != pilots[i]){
                    pilots[i].tell(m,self());
                } else {
                    return;
                }
            }
        }
    }

    @Override
    protected void requestGrantedMessage(RequestGrantedMessage m) {
        //TO CHECK IF ALL ARE RECIEVED
        m.getRequester().tell(m,self());
    }

    @Override
    protected void completedMessage(CompletedMessage m) {
        noFlyPoints.remove(m.getLocation());
        for (int i = 0; i < maxNumberOfDrones; i++) {
            if(usedAltitudes[i]){
                pilots[i].tell(m, self());
            }
        }
    }

    private void flightControlExceptionMessage(FlightControlExceptionMessage m){
        log.error("Error in SimplePilot: " + m.getMessage());
        reporterRef.tell(m,self());
    }

    @Override
    protected void shutDownMessage(ShutDownMessage m) {
        //TO DO
        self().tell(PoisonPill.getInstance(), sender());
    }

    @Override
    protected void removeDroneMessage(RemoveDroneMessage m) {

    }

    @Override
    protected void emergencyLandingMessage(EmergencyLandingMessage m) {

    }

    @Override
    protected void cancelControlMessage(CancelControlMessage m) {

    }
}
