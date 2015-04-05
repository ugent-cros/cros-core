package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.DroneCommander;
import drones.models.Fleet;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control for one single drone = pilot of the drone.
 */
public abstract class Pilot extends FlightControl{

    protected Drone drone = null;
    protected DroneCommander dc;
    protected double cruisingAltitude = 0;
    protected boolean linkedWithControlTower;

    public Pilot(ActorRef actorRef, Drone drone, boolean linkedWithControlTower) {
        super(actorRef);
        this.drone = drone;
        this.linkedWithControlTower = linkedWithControlTower;
        dc = Fleet.getFleet().getCommanderForDrone(drone);

        setHandledMessages();
    }

    /**
     * Use only for testing!
     */
    public Pilot(ActorRef actorRef, DroneCommander dc, boolean linkedWithControlTower) {
        super(actorRef);
        this.dc = dc;
        this.linkedWithControlTower = linkedWithControlTower;
    }

    private void setHandledMessages(){
        //subscribe to messages
        dc.subscribeTopic(self(), NavigationStateChangedMessage.class);
        dc.subscribeTopic(self(), LocationChangedMessage.class);


        receive(ReceiveBuilder.
                        match(SetCruisingAltitudeMessage.class, s -> setCruisingAltitude(s)).
                        match(NavigationStateChangedMessage.class, s -> navigateHomeStateChanged(s)).
                        match(LocationChangedMessage.class, s -> locationChanged(s)).
                        match(RequestForLandingMessage.class, s -> requestForLandingMessage(s)).
                        match(RequestForLandingAckMessage.class, s -> requestForLandingAckMessage(s)).
                        match(LandingCompletedMessage.class, s-> landingCompletedMessage(s)).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    private void setCruisingAltitude(SetCruisingAltitudeMessage s){
        cruisingAltitude = s.getCruisingAltitude();
    }

    protected abstract void navigateHomeStateChanged(NavigationStateChangedMessage m);

    protected abstract void locationChanged(LocationChangedMessage m);

    protected abstract void requestForLandingMessage(RequestForLandingMessage m);

    protected abstract void requestForLandingAckMessage(RequestForLandingAckMessage m);

    protected abstract void landingCompletedMessage(LandingCompletedMessage m);
}
