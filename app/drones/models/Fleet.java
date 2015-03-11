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

    private Map<String, Drone> drones;

    public Fleet(){
        drones = new HashMap<>();
    }

    public Drone createBepop(String name, String ip, boolean indoor){
        if(drones.containsKey(name))
            return null;
        else {
            ActorRef ref = Akka.system().actorOf(Props.create(Bepop.class, () -> new Bepop(ip, indoor)));
            Drone d = new Drone(ref);
            drones.put(name, d);
            return d;
        }
    }

    public Drone getDrone(String name){
        return drones.get(name);
    }
}
