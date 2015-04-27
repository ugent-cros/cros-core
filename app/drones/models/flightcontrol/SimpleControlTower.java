package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.actor.Props;
import drones.models.Location;
import drones.models.flightcontrol.messages.*;
import drones.models.scheduler.messages.to.FlightCanceledMessage;
import drones.models.scheduler.messages.to.FlightCompletedMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Simple Control Tower
 * DO NOT ADD A DRONE WITHIN THE NO FLY RANGE OF THE LOCATION WHERE ANOTHER DRONE WANTS TO LAND/TAKE OFF
 * <p>
 * Created by Sander on 15/04/2015.
 */
public class SimpleControlTower extends ControlTower {

    //range between the drone can fly
    private double maxCruisingAltitude;
    private double minCruisingAltitude;

    private int maxNumberOfDrones;
    private int numberOfDrones = 0;

    //drones
    //The index in the list determines the height. (index 0: minCruisingAltitude, index n: maxCruisingAltitude)
    private Long[] drones;
    //pilots
    private ActorRef[] pilots;

    //List of current noFlyPoints of all SimplePilots
    private List<Location> noFlyPoints;
    //HashMap to count how many pilots already granted the request
    private HashMap<RequestMessage, List<Long>> requestGrantedCount;

    private boolean started = false;

    public SimpleControlTower(ActorRef reporterRef, double maxCruisingAltitude, double minCruisingAltitude, int maxNumberOfDrones) {
        super(reporterRef);
        this.maxCruisingAltitude = maxCruisingAltitude;
        this.minCruisingAltitude = minCruisingAltitude;
        this.maxNumberOfDrones = maxNumberOfDrones;

        drones = new Long[maxNumberOfDrones];
        pilots = new ActorRef[maxNumberOfDrones];

        blocked = false;
    }

    @Override
    public void startFlightControlMessage() {
        started = true;

        //start all pilots
        for (int i = 0; i < maxNumberOfDrones; i++) {
            if (pilots[i] != null) {
                pilots[i].tell(new StartFlightControlMessage(), self());
            }
        }
    }

    @Override
    protected void stopFlightControlMessage(StopFlightControlMessage m) {
        if (!blocked) {
            blocked = true;

            //stop all pilots
            for (int i = 0; i < maxNumberOfDrones; i++) {
                if (pilots[i] != null) {
                    pilots[i].tell(new StopFlightControlMessage(), self());
                }
            }
        }
        //stop
        getContext().stop(self());
    }

    @Override
    protected void flightControlExceptionMessage(FlightControlExceptionMessage m) {
        blocked = true;
        reporterRef.tell(m, self());
    }

    private double getCruisingAltitudeForIndex(int i) {
        return minCruisingAltitude + i * (maxCruisingAltitude - minCruisingAltitude) / (maxNumberOfDrones - 1);
    }

    @Override
    protected void addDroneMessage(AddDroneMessage m) {
        if(blocked){
            return;
        }

        if (numberOfDrones == maxNumberOfDrones) {
            reporterRef.tell(new ControlTowerFullMessage(m), self());
            return;
        }

        //find available cruising altitude
        for (int i = 0; i < maxNumberOfDrones; i++) {
            if (pilots[i] != null) {
                numberOfDrones++;
                drones[i] = m.getDroneId();
                final double cruisingAltitude = getCruisingAltitudeForIndex(i);

                //start actor simple pilot
                pilots[i] = getContext().actorOf(
                        Props.create(SimplePilot.class,
                                () -> new SimplePilot(self(),m.getDroneId(),true,m.getWaypoints())));

                if(started){
                    // add no fly point to pilot
                    for (Location noFlyPoint: noFlyPoints){
                        pilots[i].tell(new AddNoFlyPointMessage(noFlyPoint),self());
                    }
                    // add granted count to waiting requests
                    for (RequestMessage requestMessage : requestGrantedCount.keySet()){
                        requestGrantedCount.get(requestMessage).add(m.getDroneId());
                    }
                    pilots[i].tell(new StartFlightControlMessage(), self());
                }
            }
        }

    }

    @Override
    protected void removeDroneMessage(RemoveDroneMessage m) {
        if(blocked){
            return;
        }

        if(removeDrone(m.getDroneId())){
            reporterRef.tell(new RemoveDroneCompletedMessage(m.getDroneId()),self());
        }
    }

    private boolean removeDrone(Long droneId){
        if(blocked){
            return false;
        }

        int i = findIndexForDroneId(droneId);
        if(i<0){
            return false;
        }

        //send stop message
        pilots[i].tell(new StopFlightControlMessage(), self());

        numberOfDrones--;

        //adjust hashmap for granted count
        for (RequestMessage requestMessage : requestGrantedCount.keySet()){
            //check if requestMessage is created by the drone that will be removed
            if(requestMessage.getRequester() == pilots[i]){
                noFlyPoints.remove(requestMessage.getLocation());
                requestGrantedCount.remove(requestMessage);
            } else {
                //remove droneId from granted count
                requestGrantedCount.get(requestMessage).remove(droneId);

                //check if this is the last drone which one was waiting
                if(requestGrantedCount.get(requestMessage).size() == numberOfDrones - 1){
                    requestGrantedCount.remove(requestMessage);
                    requestMessage.getRequester().tell(new RequestGrantedMessage(droneId,requestMessage),self());
                }
            }
        }

        //remove
        drones[i] = null;
        pilots[i] = null;

        return true;
    }

    @Override
    protected void flightCompletedMessage(FlightCompletedMessage m) {
        reporterRef.tell(m,self());

        //remove and shut down
        removeDrone(m.getDroneId());
    }

    @Override
    protected void flightCanceledMessage(FlightCanceledMessage m) {
        reporterRef.tell(m,self());
    }

    @Override
    protected void requestMessage(RequestMessage m) {
        if(blocked){
            return;
        }

        noFlyPoints.add(m.getLocation());

        if(numberOfDrones <= 1){
            m.getRequester().tell(new RequestGrantedMessage(m.getDroneId(),m),self());
            return;
        }

        int indexRequester = findIndexForDroneId(m.getDroneId());
        if(indexRequester<0){
            return;
        }

        requestGrantedCount.put(m, new ArrayList<>());
        for (int i = 0; i < maxNumberOfDrones; i++) {
            //send request message to each simple pilot with a lower cruisingAltitude
            if (pilots[i] != null &&  m.getRequester() != pilots[i]) {
                if(getCruisingAltitudeForIndex(indexRequester) < getCruisingAltitudeForIndex(i)){
                    pilots[i].tell(m, self());
                } else {
                    requestGrantedCount.get(m).add(drones[i]);
                }
            }
        }
    }

    @Override
    protected void requestGrantedMessage(RequestGrantedMessage m) {
        if(blocked){
            return;
        }

        int indexGranter = findIndexForDroneId(m.getDroneId());
        if(indexGranter<0){
            return;
        }

        //add drone to granted count
        requestGrantedCount.get(m).add(drones[indexGranter]);

        //check if this is the last drone which one was waiting
        if(requestGrantedCount.get(m).size() == numberOfDrones - 1){
            requestGrantedCount.remove(m);
            m.getRequestMessage().getRequester().tell(m,self());
        }
    }

    @Override
    protected void completedMessage(CompletedMessage m) {
        if(blocked){
            return;
        }

        //remove
        noFlyPoints.remove(m.getLocation());

        //tell to all other pilots
        for (int i = 0; i < maxNumberOfDrones; i++) {
            if(pilots[i] != null){
                pilots[i].tell(m, self());
            }
        }
    }

    private int findIndexForDroneId(Long id) {
        int index = Arrays.asList(drones).indexOf(id);
        if(index < 0){
            handleErrorMessage("Can not find actorRef for drone id");
        }
        return index;
    }
}
