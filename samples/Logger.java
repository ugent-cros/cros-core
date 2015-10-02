import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import droneapi.messages.FlyingStateChangedMessage;
import droneapi.messages.LocationChangedMessage;

/**
 * Created by yasser on 30/04/15.
 */
public class Logger extends AbstractActor {

    public Logger() {
        receive(ReceiveBuilder
                        .match(LocationChangedMessage.class, m -> printLocation(m))
                        .match(FlyingStateChangedMessage.class, m -> printFlyingState(m))
                        .build()
        );
    }


    private void printLocation(LocationChangedMessage loc) {
        System.out.println("Moved to " + loc.getLatitude() + "°, "
                + loc.getLongitude() + "°");
    }

    private void printFlyingState(FlyingStateChangedMessage state) {
        System.out.println("Drone started " + state.getState().name());
    }
}
