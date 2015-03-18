import akka.actor.ActorRef;
import akka.actor.Props;
import drones.models.scheduler.SimpleScheduler;
import play.Application;
import play.GlobalSettings;
import play.libs.Akka;

/**
 * Created by matthias on 21/02/2015.
 */
public class Global extends GlobalSettings {

    @Override
    public void onStart(Application application) {
        super.onStart(application);
        startDroneScheduler();
    }

    @Override
    public void onStop(Application application) {
        super.onStop(application);
        stopDroneScheduler();
    }


    private static ActorRef scheduler;

    public static void startDroneScheduler(){
        if(scheduler != null) return;
        scheduler = Akka.system().actorOf(Props.create(SimpleScheduler.class),"Scheduler");
    }

    public static void stopDroneScheduler(){
        Akka.system().stop(scheduler);
    }

    /**
     * Get an actor reference to the drone scheduler
     * @return
     */
    public static ActorRef getDroneScheduler(){
        return scheduler;
    }

}
