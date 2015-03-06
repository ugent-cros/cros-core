package controllers;

import com.avaje.ebean.Ebean;
import models.Drone;
import models.User;
import play.*;
import play.mvc.*;

import views.html.*;

import java.util.ArrayList;
import java.util.List;

public class Application extends Controller {

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public static Result initDb() {
        Drone.find.all().forEach(d -> d.delete());
        User.find.all().forEach(d -> d.delete());

        List<Drone> drones = new ArrayList<>();
        drones.add(new Drone("fast drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.7"));
        drones.add(new Drone("strong drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.8"));
        drones.add(new Drone("cool drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.9"));
        drones.add(new Drone("clever drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.10"));

        Ebean.save(drones);

        List<User> users = new ArrayList<>();
        users.add(new User("cros@test.be", "freddy", "cros", "tester"));

        Ebean.save(users);

        return ok();
    }

}
