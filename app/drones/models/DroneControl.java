package drones.models;

import scala.concurrent.Future;

/**
 * Created by Cedric on 3/8/2015.
 */
public interface DroneControl {
    Future<Boolean> init();
    Future<Boolean> takeOff();
    Future<Boolean> land();
}
