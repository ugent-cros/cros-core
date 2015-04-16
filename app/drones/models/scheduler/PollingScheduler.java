package drones.models.scheduler;

import akka.actor.Cancellable;
import drones.models.scheduler.messages.to.ScheduleMessage;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by Ronald on 16/04/2015.
 */
public class PollingScheduler extends AdvancedScheduler{

    private static final FiniteDuration SCHEDULE_INTERVAL = Duration.create(3, TimeUnit.SECONDS);
    private Cancellable scheduleTimer;

    public PollingScheduler(){
        super();
        // Program the Akka Scheduler to continuously send ScheduleMessages.
        scheduleTimer = context().system().scheduler().schedule(
                Duration.Zero(),
                SCHEDULE_INTERVAL,
                new Runnable() {
                    @Override
                    public void run() {
                        self().tell(new ScheduleMessage(), self());
                    }
                },
                context().system().dispatcher()
        );
    }
}
