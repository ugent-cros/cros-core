package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.util.Timeout;
import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.messages.BatteryPercentageChangedMessage;
import drones.models.*;
import models.*;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import utilities.ControllerHelper;
import utilities.MessageWebSocket;
import utilities.TestWebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Application extends Controller {

    public static final ControllerHelper.Link homeLink = new ControllerHelper.Link("home", controllers.routes.Application.index().url());

    public static Result index() {

        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.Application.index().url()));
        links.add(new ControllerHelper.Link("drone", controllers.routes.DroneController.getAll().url()));
        links.add(new ControllerHelper.Link("assignment", controllers.routes.AssignmentController.getAll().url()));
        links.add(new ControllerHelper.Link("user", controllers.routes.UserController.getAll().url()));
        links.add(new ControllerHelper.Link("basestation", controllers.routes.BasestationController.getAll().url()));
        links.add(new ControllerHelper.Link("login", controllers.routes.SecurityController.login().url()));
        links.add(new ControllerHelper.Link("datasocket", controllers.routes.Application.testSocket().url()));

        ObjectNode node = Json.newObject();
        for(ControllerHelper.Link link : links)
            node.put(link.getRel(), link.getPath());

        ObjectNode root = Json.newObject();
        root.put("home", node);

        return ok(root);

    }

    public static Result initDb() {
        Drone.FIND.all().forEach(d -> d.delete());
        Assignment.FIND.all().forEach(d -> d.delete());
        User.FIND.all().forEach(d -> d.delete());
        Basestation.FIND.all().forEach(d -> d.delete());

        List<Drone> drones = new ArrayList<>();
        DroneType bepop = new DroneType("ARDrone3", "bepop");
        DroneType ardrone2 = new DroneType("ARDrone2", "ARDrone2");
        drones.add(new Drone("old drone", Drone.Status.AVAILABLE, ArDrone2Driver.ARDRONE2_TYPE,  "address1"));
        drones.add(new Drone("fast drone", Drone.Status.AVAILABLE, bepop,  "address1"));
        drones.add(new Drone("strong drone", Drone.Status.AVAILABLE, bepop,  "address2"));
        drones.add(new Drone("cool drone", Drone.Status.AVAILABLE, bepop,  "address3"));
        drones.add(new Drone("clever drone", Drone.Status.AVAILABLE, bepop, "address4"));

        Ebean.save(drones);

        List<User> users = new ArrayList<>();
        User user = new User("cros@test.be", "freddy", "cros", "tester");
        users.add(user);
        users.add(new User("admin@drone-drinks.be", "drones", "first", "last"));
        users.get(0).setRole(User.Role.ADMIN);

        Ebean.save(users);

        Checkpoint checkpoint1 = new Checkpoint(51.023144,3.709484,3);
        Checkpoint checkpoint2 = new Checkpoint(51.022562, 3.709441,3);
        Checkpoint checkpoint3 = new Checkpoint(51.022068, 3.709945,3);
        Checkpoint checkpoint4 = new Checkpoint(51.022566, 3.710428,3);
        List<Checkpoint> checkpoints = new ArrayList<>();
        checkpoints.add(checkpoint1);
        checkpoints.add(checkpoint2);
        checkpoints.add(checkpoint3);
        checkpoints.add(checkpoint4);
        Assignment assignment = new Assignment(checkpoints, user);
        assignment.save();

        new Basestation("testing", 5.0,6.0,7.0).save();

        return ok();
    }

    private static Drone testDroneEntity;

    public static F.Promise<Result> initDrone(String ip, boolean bebop) {
        Drone droneEntity;
        if(bebop) {
            droneEntity = new Drone("bepop", Drone.Status.AVAILABLE, BepopDriver.BEPOP_TYPE, ip);
        } else {
            droneEntity = new Drone("ardrone2", Drone.Status.AVAILABLE, ArDrone2Driver.ARDRONE2_TYPE, ip);
        }

        droneEntity.save();

        DroneCommander d = Fleet.getFleet().getCommanderForDrone(droneEntity);
        return F.Promise.wrap(d.init()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            result.put("id", droneEntity.getId());
            return ok(result);
        });
    }

    public static Result subscribeMonitor(){
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(testDroneEntity);
        ActorRef r = Akka.system().actorOf(Props.create(DroneMonitor.class), "droneMonitor");
        d.subscribeTopic(r, BatteryPercentageChangedMessage.class);

        ObjectNode result = Json.newObject();
        result.put("status", "subscribed");
        return ok(result);
    }

    public static WebSocket<String> testSocket() {
        String[] tokens = request().queryString().get("authToken");

        if (tokens == null || tokens.length != 1 || tokens[0] == null)
            return WebSocket.reject(unauthorized());

        User u = models.User.findByAuthToken(tokens[0]);
        if (u != null) {
            return WebSocket.withActor(TestWebSocket::props);
        } else {
            return WebSocket.reject(unauthorized());
        }
    }

    public static WebSocket<String> socket() {
        return WebSocket.withActor(MessageWebSocket::props);
    }

    public static Result unsubscribeMonitor(){
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(testDroneEntity);

        try {
            ActorRef r = Await.result(Akka.system().actorSelection("/user/droneMonitor").resolveOne(new Timeout(5, TimeUnit.SECONDS)),
                    new Timeout(5, TimeUnit.SECONDS).duration());

            d.unsubscribe(r);
            ObjectNode result = Json.newObject();
            result.put("status", "unsubscribed");
            return ok(result);
        } catch (Exception e) {
            ObjectNode result = Json.newObject();
            result.put("status", "not-found");
            return ok(result);
        }
    }

    public static F.Promise<Result> getBatteryPercentage(long id){
        Drone drone = Drone.FIND.byId(id);
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.getBatteryPercentage()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("batteryPercentage", v);
            return ok(result);
        });
    }

    public static F.Promise<Result> setOutdoor(boolean outdoor, long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.setOutdoor(outdoor)).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("outdoor", outdoor);
            return ok(result);
        });
    }

    public static F.Promise<Result> setMaxHeight(float meters, long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.setMaxHeight(meters)).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("maxHeight", meters);
            return ok(result);
        });
    }

    public static F.Promise<Result> setHull(boolean hull, long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.setHull(hull)).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("hull", hull);
            return ok(result);
        });
    }

    public static F.Promise<Result> flatTrim(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.flatTrim()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }

    public static F.Promise<Result> calibrate(boolean hull, boolean outdoor, long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.calibrate(outdoor, hull)).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }

    public static F.Promise<Result> moveToLocation(double latitude, double longitude, double altitude, long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.moveToLocation(latitude, longitude, altitude)).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "requested");
            ObjectNode locationResult = Json.newObject();
            locationResult.put("latitude", latitude);
            locationResult.put("longitude", longitude);
            locationResult.put("altitude", altitude);
            result.put("location", locationResult);
            return ok(result);
        });
    }

    public static F.Promise<Result> moveVector(double vx, double vy, double vz, double vr, long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.move3d(vx, vy, vz, vr)).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "requested");
            ObjectNode locationResult = Json.newObject();
            locationResult.put("vx", vx);
            locationResult.put("vy", vy);
            locationResult.put("vz", vz);
            locationResult.put("vr", vr);
            result.put("location", locationResult);
            return ok(result);
        });
    }

    public static F.Promise<Result> getLocation(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.getLocation()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("long", v.getLongitude());
            result.put("lat", v.getLatitude());
            result.put("altitude", v.getHeight());
            return ok(result);
        });
    }

    public static F.Promise<Result> getAltitude(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.getAltitude()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("altitude", v);
            return ok(result);
        });
    }

    public static F.Promise<Result> getVersion(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.getVersion()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("softwareVersion", v.getSoftware());
            result.put("hardwareVersion", v.getHardware());
            return ok(result);
        });
    }

    public static F.Promise<Result> getSpeed(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.getSpeed()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("vx", v.getVx());
            result.put("vy", v.getVy());
            result.put("vz", v.getVz());
            return ok(result);
        });
    }

    public static F.Promise<Result> getRotation(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.getRotation()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("yaw", v.getYaw());
            result.put("pitch", v.getPitch());
            result.put("roll", v.getRoll());
            return ok(result);
        });
    }

    public static F.Promise<Result> takeOff(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.takeOff()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }

    public static F.Promise<Result> land(long id){
        Drone drone = Drone.FIND.where().eq("id", id).findUnique();
        DroneCommander d = Fleet.getFleet().getCommanderForDrone(drone);
        return F.Promise.wrap(d.land()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }

    public static Result preflight(String all) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        response().setHeader("Allow", "*");
        response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, X-AUTH-TOKEN");
        return ok();
    }

}
