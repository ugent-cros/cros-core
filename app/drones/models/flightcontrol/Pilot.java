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

    protected Drone drone;
    protected DroneCommander dc;
    protected double altitude;
    protected boolean linkedWithControlTower;

    public Pilot(ActorRef actorRef, Drone drone, boolean withControlTower) {
        super(actorRef);
        this.drone = drone;
        this.linkedWithControlTower = withControlTower;
        dc = Fleet.getFleet().getCommanderForDrone(drone);

        //subscribe to messages
        dc.subscribeTopic(self(), NavigationStateChangedMessage.class);
        dc.subscribeTopic(self(), LocationChangedMessage.class);


        receive(ReceiveBuilder.
                        match(SetAltitudeMessage.class, s -> setAltitude(s)).
                        match(NavigationStateChangedMessage.class, s -> navigateHomeStateChanged(s)).
                        match(LocationChangedMessage.class, s -> locationChanged(s)).
                        match(RequestForLandingMessage.class, s -> requestForLandingMessage(s)).
                        match(RequestForLandingAckMessage.class, s -> requestForLandingAckMessage(s)).
                        match(LandingCompletedMessage.class, s-> landingCompletedMessage(s)).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    private void setAltitude(SetAltitudeMessage s){
        altitude = s.getAltitude();
    }

    protected abstract void navigateHomeStateChanged(NavigationStateChangedMessage m);

    protected abstract void locationChanged(LocationChangedMessage m);

    protected abstract void requestForLandingMessage(RequestForLandingMessage m);

    protected abstract void requestForLandingAckMessage(RequestForLandingAckMessage m);

    protected abstract void landingCompletedMessage(LandingCompletedMessage m);
}
