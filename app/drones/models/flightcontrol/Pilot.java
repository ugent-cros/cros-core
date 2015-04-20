package drones.models.flightcontrol;

import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import akka.japi.pf.UnitPFBuilder;
import drones.messages.FlyingStateChangedMessage;
import drones.messages.LocationChangedMessage;
import drones.messages.NavigationStateChangedMessage;
import drones.models.DroneCommander;
import drones.models.DroneException;
import drones.models.Fleet;
import drones.models.flightcontrol.messages.*;
import models.Drone;
import scala.concurrent.Await;

import java.util.concurrent.TimeoutException;

/**
 * Created by Sander on 18/03/2015.
 *
 * Flight control for one single drone = pilot of the drone.
 */
public abstract class Pilot extends FlightControl{

    protected Long droneId;
    protected DroneCommander dc;
    protected double cruisingAltitude = 0;
    protected boolean linkedWithControlTower;

    public Pilot(ActorRef reporterRef, Long droneId, boolean linkedWithControlTower) {
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
        this.droneId = new Long(0);
        this.linkedWithControlTower = linkedWithControlTower;

        setSubscribeMessages();
    }

    private void setSubscribeMessages(){
        //subscribe to messages
        dc.subscribeTopic(self(), NavigationStateChangedMessage.class);
        dc.subscribeTopic(self(), LocationChangedMessage.class);
        dc.subscribeTopic(self(), FlyingStateChangedMessage.class);
    }

    @Override
    protected UnitPFBuilder<Object> createListeners() {
        return ReceiveBuilder.
                match(SetCruisingAltitudeMessage.class, s -> setCruisingAltitude(s)).
                match(NavigationStateChangedMessage.class, s -> navigationStateChanged(s)).
                match(LocationChangedMessage.class, s -> locationChanged(s)).
                match(FlyingStateChangedMessage.class, s -> flyingStateChanged(s));
    }

    private void setCruisingAltitude(SetCruisingAltitudeMessage s){
        cruisingAltitude = s.getCruisingAltitude();
        try {
            Await.ready(dc.setMaxHeight((float) cruisingAltitude), MAX_DURATION_MESSAGE);
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
            reporterRef.tell(new DroneException("Failed to set max height after SetCruisingAltitudeMessage"),self());
        }
    }

    protected abstract void navigationStateChanged(NavigationStateChangedMessage m);

    protected abstract void flyingStateChanged(FlyingStateChangedMessage m);

    protected abstract void locationChanged(LocationChangedMessage m);
}
