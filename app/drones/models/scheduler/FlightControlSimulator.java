package drones.models.scheduler;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.flightcontrol.messages.StartFlightControlMessage;
import drones.models.flightcontrol.messages.StopFlightControlMessage;
import drones.models.scheduler.messages.to.FlightCanceledMessage;
import drones.models.scheduler.messages.to.FlightCompletedMessage;
import models.Checkpoint;
import models.Drone;
import play.Logger;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 26/04/2015.
 */
public class FlightControlSimulator extends UntypedActor {

    private ActorRef scheduler;
    private long droneId;
    private boolean started = false;
    private boolean completed = false;
    private DroneCommander commander;
    private static final FiniteDuration TIME_OUT = Duration.create(10,TimeUnit.SECONDS);

    private static int flyTime = 500;
    private int actualFlyTime;
    public static void setFlyTime(int flyTime){
        FlightControlSimulator.flyTime = flyTime;
    }

    public FlightControlSimulator(ActorRef scheduler, long droneId, boolean linkedWithControlTower, List<Checkpoint> route) {
        this.scheduler = scheduler;
        this.droneId = droneId;
        this.actualFlyTime = flyTime;
        Drone drone = Drone.FIND.byId(droneId);
        commander = Fleet.getFleet().getCommanderForDrone(drone);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof StartFlightControlMessage){
            Logger.debug("FLIGHTCONTROL START RECEIVED");
            if(!started) {
                started = true;
                Await.ready(commander.takeOff(),TIME_OUT);
                ExecutionContext ec = getContext().dispatcher();
                getContext().system().scheduler()
                        .scheduleOnce(Duration.create(flyTime, TimeUnit.SECONDS), self(), true, ec, self());
                Logger.debug("FLIGHTCONTROL START EXECUTED");
            }
            return;
        }
        if(message instanceof StopFlightControlMessage){
            Logger.debug("FLIGHTCONTROL STOP RECEIVED");
            if(!completed){
                Logger.debug("FLIGHTCONTROL FLIGHT CANCELED");
                Await.ready(commander.land(), TIME_OUT);
                scheduler.tell(new FlightCanceledMessage(droneId),self());
            }
            Logger.debug("FLIGHTCONTROL STOPPED");
            getContext().stop(self());
            return;
        }
        if(message instanceof Boolean){
            Logger.debug("FLIGHTCONTROL COMPLETED");
            completed = true;
            scheduler.tell(new FlightCompletedMessage(droneId,null),self());
            return;
        }
        unhandled(message);
    }
}
