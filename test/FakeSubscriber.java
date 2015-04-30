import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import messages.StopMessage;

import java.io.Serializable;

import controllers.*;

/**
 * Created by Cedric on 4/2/2015.
 */
public class FakeSubscriber extends AbstractActor {

    public static class LastMessageRequest implements Serializable{

    }

    public static class LastMessageResponse implements Serializable{
        private Object value;

        public LastMessageResponse(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    private Object lastMessage;

    public FakeSubscriber(){
        receive(ReceiveBuilder
                .match(StopMessage.class, s -> {
                    getContext().stop(self());
                })
                .match(LastMessageRequest.class, m -> {
                    sender().tell(new LastMessageResponse(lastMessage), self());
                    lastMessage = null;
                })
                .matchAny(m -> {
                    lastMessage = m;
                }).build());
    }
}
