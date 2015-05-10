package drones.flightcontrol;

import akka.actor.ActorRef;
import akka.actor.Props;
import drones.flightcontrol.messages.*;
import drones.scheduler.messages.to.FlightCanceledMessage;
import drones.scheduler.messages.to.FlightCompletedMessage;
import droneapi.model.properties.Location;

import java.util.*;

/**
 * Basic implementation of a ControlTower. When a new flight is added, it creates a SimplePilot for it and
 * assigns a cruising altitude to it. When a RequestMessage is received from a SimplePilot it will answer
 * with a GrantedMessage when all SimplePilots with a lower cruising altitude has granted the request.
 *
 * !!! WARNING 1: Do not add a drone within the NoFlyRange of the location where another drone wants to land
 * or take off.
 *
 * !!! WARNING 2: The SimpleControlTower makes use of the SimplePilot class, see warnings there.
 *
 * Created by Sander on 15/04/2015.
 */
public class SimpleControlTower extends ControlTower {

    //range between the drone can fly
    private double maxCruisingAltitude;
    private double minCruisingAltitude;

    //Map droneId on a pilot and a cruising altitude
    //Map<droneId,Pair<SimplePilot,cruisingAltitude>
    private Map<Long,Pair<ActorRef,Double>> drones = new HashMap<>();

    private List<Double> availableCruisingAltitudes = new ArrayList<>();

    //List of current noFlyPoints of all SimplePilots
    //List<Pair<Location,cruisingAltitude>
    private List<Pair<Location,Double>> noFlyPoints = new ArrayList<>();
    //HashMap to count how many pilots already granted the request
    private Map<RequestMessage, List<Long>> requestGrantedCount = new HashMap<>();

    private boolean started = false;

    //List of drones that needs to sends a FlightCanceledMessage before they can be removed from the ControlTower
    private List<Long> waitForFlightCanceledMessage = new ArrayList<>();

    private boolean waitForShutDown = false;

    //list to check if all pilots has stopped
    private List<FlightCanceledMessage> flightCanceledMessages = new ArrayList<>();

    /**
     *
     * @param reporterRef actor to report the outgoing messages
     * @param maxCruisingAltitude maximum cruising altitude that the drones can fly
     * @param minCruisingAltitude minimum cruising altitude that the drones can fly
     * @param maxNumberOfDrones maximum number of drones that the controlTower can handle.
     */
    public SimpleControlTower(ActorRef reporterRef, double maxCruisingAltitude, double minCruisingAltitude, int maxNumberOfDrones) {
        super(reporterRef);
        this.maxCruisingAltitude = maxCruisingAltitude;
        this.minCruisingAltitude = minCruisingAltitude;

        setAvailableCruisingAltitudes(maxNumberOfDrones);

        blocked = false;
    }

    @Override
    public void startFlightControlMessage() {
        started = true;

        //start all pilots
        tellAllPilots(new StartFlightControlMessage());
    }

    /**
     * Tell message to al the pilots in the ControlTower.
     *
     * @param message Message to be sent
     */
    private void tellAllPilots(Object message){
        for(Pair<ActorRef,Double> pair : drones.values()){
            pair.getKey().tell(message, self());
        }
    }

    @Override
    protected void stopFlightControlMessage(StopFlightControlMessage m) {
        if (!blocked) {
            blocked = true;

            //stop all pilots
            for(Long droneId : drones.keySet()){
                if(!waitForFlightCanceledMessage.contains(droneId)){
                    waitForFlightCanceledMessage.add(droneId);
                    drones.get(droneId).getKey().tell(new StopFlightControlMessage(), self());
                }
            }
            waitForShutDown = true;
        } else {
            //stop
            getContext().stop(self());
        }
    }

    @Override
    protected void flightControlExceptionMessage(FlightControlExceptionMessage m) {
        blocked = true;
        reporterRef.tell(m, self());
    }

    /**
     * Fill list with available cruising altitudes
     * @param maxNumberOfDrones
     */
    private void setAvailableCruisingAltitudes(int maxNumberOfDrones) {
        if (maxNumberOfDrones == 1) {
            availableCruisingAltitudes.add(minCruisingAltitude + (maxCruisingAltitude - minCruisingAltitude) / 2);
        } else {
            for(int i = 0; i < maxNumberOfDrones; i++){
               availableCruisingAltitudes.add(minCruisingAltitude + i * (maxCruisingAltitude - minCruisingAltitude) / (maxNumberOfDrones - 1));
            }
        }
    }

    @Override
    protected void addFlightMessage(AddFlightMessage m) {
        if (blocked) {
            return;
        }

        if (availableCruisingAltitudes.isEmpty()) {
            reporterRef.tell(new ControlTowerFullMessage(m), self());
            return;
        }

        //find available cruising altitude
        final double cruisingAltitude = availableCruisingAltitudes.get(0);
        availableCruisingAltitudes.remove(cruisingAltitude);

        //make list with all noFlyPoint with a lower cruisingAltitude
        List<Location> list = new ArrayList<>();
        for(Pair<Location,Double> pair: noFlyPoints){
            if(pair.getValue() < cruisingAltitude){
                list.add(pair.getKey());
            }
        }

        //create actor
        ActorRef pilot = getContext().actorOf(
                Props.create(SimplePilot.class,
                        () -> new SimplePilot(self(), m.getDroneId(), true, m.getWaypoints(), cruisingAltitude, list)));

        //add drone to map
        drones.put(m.getDroneId(), new Pair<>(pilot,cruisingAltitude));

        if(started){
            // add granted count to waiting requests
            for (RequestMessage requestMessage : requestGrantedCount.keySet()) {
                requestGrantedCount.get(requestMessage).add(m.getDroneId());
            }
            pilot.tell(new StartFlightControlMessage(), self());
        }
    }

    @Override
    protected void removeFlightMessage(RemoveFlightMessage m) {
        if (blocked) {
            return;
        }

        removeDrone(m.getDroneId());
    }

    private boolean removeDrone(long droneId) {
        if (blocked) {
            return false;
        }

        //send stop message
        drones.get(droneId).getKey().tell(new StopFlightControlMessage(), self());
        waitForFlightCanceledMessage.add(droneId);

        //remove from drones map is done when FlightCanceledMessage is received
        return true;
    }

    @Override
    protected void flightCompletedMessage(FlightCompletedMessage m) {
        reporterRef.tell(m, self());

        //remove and shut down
        removeDrone(m.getDroneId());
    }

    @Override
    protected void flightCanceledMessage(FlightCanceledMessage m) {
        if(waitForFlightCanceledMessage.contains(m.getDroneId())){
            waitForFlightCanceledMessage.remove(m.getDroneId());

            //adjust hashmap for granted count
            ActorRef pilot = drones.get(m.getDroneId()).getKey();

            Iterator<RequestMessage> it = requestGrantedCount.keySet().iterator(); //use iterator because otherwise ConcurrentModificationException
            while(it.hasNext()){
                RequestMessage requestMessage = it.next();
                //check if requestMessage is created by the drone that will be removed
                if (requestMessage.getRequester() == pilot) {
                    tellAllPilots(new CompletedMessage(requestMessage));
                    noFlyPoints.remove(requestMessage.getLocation());
                    //Remove from hasmap requestGrantedCount
                    it.remove();
                } else {
                    //remove droneId from granted count
                    requestGrantedCount.get(requestMessage).remove(m.getDroneId());

                    //check if this is the last drone which one was waiting
                    if (requestGrantedCount.get(requestMessage).size() == drones.size() - 2) {
                        //Remove from hasmap requestGrantedCount
                        it.remove();
                        requestMessage.getRequester().tell(new RequestGrantedMessage(m.getDroneId(), requestMessage), self());
                    }
                }
            }

            //remove drone
            availableCruisingAltitudes.add(drones.get(m.getDroneId()).getValue());
            drones.remove(m.getDroneId());

            //Check if wait for ShutDown and if all messages are received
            if(waitForShutDown){
                if(waitForFlightCanceledMessage.isEmpty()) {
                    waitForShutDown = false;
                    reporterRef.tell(new FlightControlCanceledMessage(), self());
                    //stop
                    getContext().stop(self());
                }
            } else {
                reporterRef.tell(new RemoveFlightCompletedMessage(m.getDroneId()), self());
            }
        }
    }

    @Override
    protected void requestMessage(RequestMessage m) {
        if (blocked) {
            return;
        }

        noFlyPoints.add(new Pair<>(m.getLocation(),drones.get(m.getDroneId()).getValue()));

        if (drones.size() <= 1) {
            m.getRequester().tell(new RequestGrantedMessage(m.getDroneId(), m), self());
            return;
        }

        requestGrantedCount.put(m, new ArrayList<>());

        double requestedAltitude = drones.get(m.getDroneId()).getValue();

        for(Long droneId: drones.keySet()){
            Pair<ActorRef,Double> pair = drones.get(droneId);
            //send request message to each simple pilot with a lower cruisingAltitude
            if(m.getRequester() != pair.getKey()){
                if(pair.getValue() < requestedAltitude){
                    pair.getKey().tell(m,self());
                } else {
                    requestGrantedCount.get(m).add(droneId);
                }
            }
        }

        //check if list is already full (this is only the case when the drone is flying at the lowest possible cruisingaltitude)
        if (requestGrantedCount.get(m).size() == drones.size() - 1) {
            requestGrantedCount.remove(m);
            m.getRequester().tell(new RequestGrantedMessage(m.getDroneId(), m), self());
        }
    }

    @Override
    protected void requestGrantedMessage(RequestGrantedMessage m) {
        if (blocked) {
            return;
        }

        //add drone to granted count
        requestGrantedCount.get(m.getRequestMessage()).add(m.getDroneId());

        //check if this is the last drone which one was waiting
        if (requestGrantedCount.get(m.getRequestMessage()).size() == drones.size() - 1) {
            requestGrantedCount.remove(m.getRequestMessage());
            m.getRequestMessage().getRequester().tell(m, self());
        }
    }

    @Override
    protected void completedMessage(CompletedMessage m) {
        if (blocked) {
            return;
        }

        //remove
        noFlyPoints.remove(m.getLocation());

        //tell all other pilots
        tellAllPilots(m);
    }

    @Override
    protected void wayPointCompletedMessage(WayPointCompletedMessage m) {
        reporterRef.tell(m, self());
    }
}
