package drones.models;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import drones.messages.BatteryPercentageChangedMessage;

/**
 * Example subscriber implementation
 * Created by Cedric on 3/17/2015.
 */
public class DroneMonitor extends AbstractActor {
    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public DroneMonitor(){
        receive(ReceiveBuilder.match(BatteryPercentageChangedMessage.class, s -> log.info("DroneMonitor: [{}]", s.getPercent())).build());
    }
}
