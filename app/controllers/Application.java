package controllers;

import com.avaje.ebean.Ebean;
import models.*;
import play.mvc.Controller;
import play.mvc.Result;
import utilities.ControllerHelper;
import views.html.index;

import java.util.ArrayList;
import java.util.List;

public class Application extends Controller {

    public static final ControllerHelper.Link homeLink = new ControllerHelper.Link("home", controllers.routes.Application.index().url());

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public static Result initDb() {
        Drone.find.all().forEach(d -> d.delete());
        Assignment.find.all().forEach(d -> d.delete());
        User.find.all().forEach(d -> d.delete());

        List<Drone> drones = new ArrayList<>();
        drones.add(new Drone("fast drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.7"));
        drones.add(new Drone("strong drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.8"));
        drones.add(new Drone("cool drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.9"));
        drones.add(new Drone("clever drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "192.168.0.10"));

        Ebean.save(drones);

        List<User> users = new ArrayList<>();
        User user = new User("cros@test.be", "freddy", "cros", "tester");
        users.add(user);
        users.add(new User("admin@drone-drinks.be", "drones", "first", "last"));
        users.get(0).role = User.Role.ADMIN;

        Ebean.save(users);

        Checkpoint checkpoint = new Checkpoint(1,2,3);
        List<Checkpoint> checkpoints = new ArrayList<>();
        checkpoints.add(checkpoint);
        Assignment assignment = new Assignment(checkpoints, user);
        assignment.save();

        new Basestation("testing", new Checkpoint(5, 6, 7)).save();

        return ok();
    }

}
