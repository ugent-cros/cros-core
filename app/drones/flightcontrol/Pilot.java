package drones.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import droneapi.api.DroneCommander;
import droneapi.messages.FlyingStateChangedMessage;
import droneapi.messages.LocationChangedMessage;
import droneapi.messages.NavigationStateChangedMessage;
import drones.flightcontrol.messages.AddNoFlyPointMessage;
import drones.flightcontrol.messages.WaitAtWayPointCompletedMessage;
import drones.models.Fleet;
import models.Drone;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control for one single drone = pilot of the drone.
 */
public abstract class Pilot extends FlightControl{

    protected long droneId;
    protected DroneCommander dc;
    protected double cruisingAltitude = 0;
    protected boolean linkedWithControlTower;

    public Pilot(ActorRef reporterRef, long droneId, boolean linkedWithControlTower) {
        super(reporterRef);
        this.droneId = droneId;
        this.linkedWithControlTower = linkedWithControlTower;

        //get Drone
        Drone drone = Drone.FIND.byId(droneId);
        dc = Fleet.getFleet().getCommanderForDrone(drone);

        setSubscribeMessages();
    }

    /**
     * Use only for testing!
     */
    public Pilot(ActorRef reporterRef, DroneCommander dc, boolean linkedWithControlTower) {
        super(reporterRef);
        this.dc = dc;
        this.droneId = 0;
        this.linkedWithControlTower = linkedWithControlTower;

        setSubscribeMessages();
    }

    private void setSubscribeMessages(){
        //subscribe to messages
        dc.subscribeTopic(self(), FlyingStateChangedMessage.class);
        dc.subscribeTopic(self(), LocationChangedMessage.class);
        dc.subscribeTopic(self(), NavigationStateChangedMessage.class);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(FlyingStateChangedMessage.class, s -> flyingStateChanged(s)).
                match(LocationChangedMessage.class, s -> locationChanged(s)).
                match(NavigationStateChangedMessage.class, s -> navigationStateChanged(s)).
                match(WaitAtWayPointCompletedMessage.class, s -> waitAtWayPointCompletedMessage(s));
    }

    protected abstract void flyingStateChanged(FlyingStateChangedMessage m);

    protected abstract void locationChanged(LocationChangedMessage m);

    protected abstract void navigationStateChanged(NavigationStateChangedMessage m);

    protected abstract void addNoFlyPointMessage(AddNoFlyPointMessage m);

    protected abstract void waitAtWayPointCompletedMessage(WaitAtWayPointCompletedMessage m);
}
