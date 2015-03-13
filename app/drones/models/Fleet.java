package drones.models;

import akka.actor.ActorRef;
import akka.actor.Props;
import play.libs.Akka;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Cedric on 3/9/2015.
 */
public class Fleet {
    private static final Fleet fleet = new Fleet();

    public static Fleet getFleet(){
        return fleet;
    }

    private Map<String, DroneCommander> drones;

    public Fleet(){
        drones = new HashMap<>();
    }

    public DroneCommander createBepop(String name, String ip, boolean indoor){
        if(drones.containsKey(name)) {
            return drones.get(name); //TODO: check with others about this behaviour
        }  else {
            ActorRef ref = Akka.system().actorOf(Props.create(Bepop.class, () -> new Bepop(ip, indoor)));
            DroneCommander d = new DroneCommander(ref);
            drones.put(name, d);
            return d;
        }
    }

    public DroneCommander createArDrone2(String name, String ip, boolean indoor){
        if(drones.containsKey(name))
            return null;
        else {
            ActorRef ref = Akka.system().actorOf(Props.create(ArDrone2.class, () -> new ArDrone2(ip, indoor)));
            DroneCommander d = new DroneCommander(ref);
            drones.put(name, d);
            return d;
        }
    }

    public DroneCommander getDrone(String name){
        return drones.get(name);
    }
}
