package parrot.shared.protocols;

import akka.actor.AbstractActor;
import akka.actor.Status;
import akka.japi.pf.ReceiveBuilder;
import parrot.messages.PingMessage;
import parrot.shared.models.PingResult;

import java.net.InetAddress;

/**
 * Created by Cedric on 3/22/2015.
 */
public class ICMPPing extends AbstractActor {

    public final static int PING_TIMEOUT = 2000;

    public ICMPPing(){
        receive(ReceiveBuilder.match(PingMessage.class, s -> {
            String[] splitted = s.getIp().split("\\.");
            if(splitted.length != 4) {
                sender().tell(new Status.Failure(new IllegalArgumentException("IP address is in wrong format.")), self());
                return;
            }

            byte[] nums = new byte[4];
            for(int i = 0; i < 4; i++){
                nums[i] = Byte.parseByte(splitted[i]);
            }

            PingResult result = InetAddress.getByAddress(nums).isReachable(PING_TIMEOUT) ? PingResult.OK : PingResult.UNREACHABLE;
            sender().tell(result, self());
        }).build());
    }
}
