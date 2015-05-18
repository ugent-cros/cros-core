package drones.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import droneapi.api.DroneCommander;
import droneapi.messages.FlyingStateChangedMessage;
import droneapi.messages.LocationChangedMessage;
import droneapi.messages.NavigationStateChangedMessage;
import drones.flightcontrol.messages.AddNoFlyPointMessage;
import drones.flightcontrol.messages.FlightControlExceptionMessage;
import drones.flightcontrol.messages.WaitAtWayPointCompletedMessage;
import drones.models.Fleet;
import models.Drone;

/**
 * A pilot executes a flight with one drone.
 *
 * Created by Sander on 18/03/2015.
 */
public abstract class Pilot extends FlightControl{

    protected long droneId;
    protected DroneCommander dc;
    protected double cruisingAltitude = 0;
    protected boolean linkedWithControlTower;

    /**
     *
     * @param reporterRef               actor to report the outgoing messages
     * @param droneId                   drone to control
     * @param linkedWithControlTower    true if connected to a ControlTower
     */
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

    /**
     * Handles a FlyingStateChangedMessage sent by a droneActor.
     */
    protected abstract void flyingStateChanged(FlyingStateChangedMessage m);

    /**
     * Handles a LocationChangedMessage sent by a droneActor.
     */
    protected abstract void locationChanged(LocationChangedMessage m);

    /**
     * Handles a NavigationStateChangedMessage sent by a droneActor.
     */
    protected abstract void navigationStateChanged(NavigationStateChangedMessage m);

    /**
     * Add a NoFLyPoint to the pilot.
     */
    protected abstract void addNoFlyPointMessage(AddNoFlyPointMessage m);

    /**
     * Handles a WaitAtWayPointCompletedMessage sent by itself.
     */
    protected abstract void waitAtWayPointCompletedMessage(WaitAtWayPointCompletedMessage m);
}
