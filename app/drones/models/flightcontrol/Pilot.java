package drones.models.flightcontrol;

import akka.dispatch.OnSuccess;
import akka.japi.pf.ReceiveBuilder;
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

    protected boolean gpsFix = false;

    public Pilot(Drone drone) {
        super();
        this.drone = drone;
        dc = Fleet.getFleet().getCommanderForDrone(drone);

        //check GPSFix
        dc.isGPSFixed().onSuccess(new OnSuccess<Boolean>(){

            @Override
            public void onSuccess(Boolean result) throws Throwable {
                gpsFix = result;
            }
        },getContext().system().dispatcher());

        receive(ReceiveBuilder.
                        match(SetAltitudeMessage.class, s -> setAltitude(s)).
                        matchAny(o -> log.info("FlightControl message recv: [{}]", o.getClass().getCanonicalName())).build()
        );
    }

    private void setAltitude(SetAltitudeMessage s){
        altitude = s.getAltitude();
    }
}
