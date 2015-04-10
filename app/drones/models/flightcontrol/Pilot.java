package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.flightcontrol.messages.*;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control for one single drone = pilot of the drone.
 */
public abstract class Pilot extends FlightControl{

    protected Drone drone;
    protected DroneCommander dc;
    protected double cruisingAltitude = 0;
    protected boolean linkedWithControlTower;

    public Pilot(ActorRef reporterRef, Drone drone, boolean linkedWithControlTower) {
        super(reporterRef);
        this.drone = drone;
        this.linkedWithControlTower = linkedWithControlTower;
        dc = Fleet.getFleet().getCommanderForDrone(drone);

        setSubscribeMessages();
    }

    /**
     * Use only for testing!
     */
    public Pilot(ActorRef reporterRef, DroneCommander dc, boolean linkedWithControlTower) {
        super(reporterRef);
        this.dc = dc;
        this.drone = new Drone();
        this.linkedWithControlTower = linkedWithControlTower;

        setSubscribeMessages();
    }

    private void setSubscribeMessages(){
        //subscribe to messages
        dc.subscribeTopic(self(), NavigationStateChangedMessage.class);
        dc.subscribeTopic(self(), LocationChangedMessage.class);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(SetCruisingAltitudeMessage.class, s -> setCruisingAltitude(s)).
                match(NavigationStateChangedMessage.class, s -> navigateHomeStateChanged(s)).
                match(LocationChangedMessage.class, s -> locationChanged(s));
    }

    private void setCruisingAltitude(SetCruisingAltitudeMessage s){
        cruisingAltitude = s.getCruisingAltitude();
    }

    protected abstract void navigateHomeStateChanged(NavigationStateChangedMessage m);

    protected abstract void locationChanged(LocationChangedMessage m);

}
