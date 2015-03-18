package drones.models.flightcontrol;

import drones.models.DroneCommander;

/**
 * Created by Sander on 18/03/2015.
 */
public class SimplePilot extends Pilot{


    public SimplePilot(DroneCommander dc) {
        super(dc);
    }

    @Override
    public void start() {
        dc.init();
    }
}
