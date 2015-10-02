import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import droneapi.api.DroneCommander;
import droneapi.messages.FlyingStateChangedMessage;
import droneapi.messages.LocationChangedMessage;
import droneapi.model.properties.FlipType;
import droneapi.model.properties.FlyingState;
import parrot.ardrone2.ArDrone2;
import parrot.ardrone2.ArDrone2Driver;
import parrot.ardrone3.BebopDriver;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static java.lang.Thread.sleep;

public class ArDrone2Example {

	public static void main(String args[]) throws Exception {

		BebopDriver driver = new BebopDriver();
		ArDrone2Driver ar2Driver = new ArDrone2Driver();
		final DroneCommander commander = new DroneCommander("192.168.1.175", driver);

		ActorSystem system = ActorSystem.create();

		ActorRef logger = system.actorOf(Props.create(Logger.class));

		commander.subscribeTopics(logger, new Class[]{FlyingStateChangedMessage.class, LocationChangedMessage.class});
		Future<Void> init = commander.init();
		init.onSuccess(new OnSuccess<Void>() {
			@Override
			public void onSuccess(Void result) throws Throwable {
				commander.takeOff();
				sleep(5000);
				commander.flip(FlipType.FRONT);
				sleep(5000);
				commander.land();
			}
		}, system.dispatcher());


		System.in.read();
		commander.land();
	}
}
