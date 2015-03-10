package drones.models;

import scala.concurrent.Future;

/**
 * Created by Cedric on 3/8/2015.
 */
public interface DroneControl {
    Future<Void> init();
    Future<Void> takeOff();
    Future<Void> land();
    Future<Void> emergency();
}
