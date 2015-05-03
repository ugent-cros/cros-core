package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import droneapi.api.DroneCommander;
import droneapi.api.DroneControl;
import droneapi.model.properties.FlipType;
import drones.models.Fleet;
import models.Drone;
import models.User;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by matthias on 25/04/2015.
 */

@Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
public class ManualDroneController extends Controller {

    private static final Map<String,BiFunction<DroneCommander, Http.Request,F.Promise<Result>>> COMMANDS;

    static {
        COMMANDS = new HashMap<>();
        COMMANDS.put("flipBack", (c,request) -> F.Promise.wrap(c.flip(FlipType.BACK)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flipFront", (c,request) -> F.Promise.wrap(c.flip(FlipType.FRONT)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flipLeft", (c,request) -> F.Promise.wrap(c.flip(FlipType.LEFT)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flipRight", (c,request) -> F.Promise.wrap(c.flip(FlipType.RIGHT)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setOutdoor", (c,request) -> F.Promise.wrap(c.setOutdoor(true)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setIndoor", (c,request) -> F.Promise.wrap(c.setOutdoor(false)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setHull", (c,request) -> F.Promise.wrap(c.setHull(true)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setNoHull", (c,request) -> F.Promise.wrap(c.setHull(false)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flatTrim", (c,request) -> F.Promise.wrap(c.flatTrim()).map(v -> ok(Json.newObject())));
        COMMANDS.put("takeOff", (c,request) -> F.Promise.wrap(c.takeOff()).map(v -> ok(Json.newObject())));
        COMMANDS.put("land", (c,request) -> F.Promise.wrap(c.land()).map(v -> ok(Json.newObject())));
        COMMANDS.put("center", (c,request) -> F.Promise.wrap(c.move3d(0, 0, 0, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveLeft", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(0, -speed, 0, 0)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("moveRight", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(0, speed, 0, 0)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("moveUp", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(0, 0, speed, 0)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("moveDown", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(0, 0, -speed, 0)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("moveForward", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(speed, 0, 0, 0)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("moveBackward", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(-speed, 0, 0, 0)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("rotateLeft", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(0, 0, 0, -speed)).map(v -> ok(Json.newObject()));
        });
        COMMANDS.put("rotateRight", (c,request) -> {
            float speed = Float.parseFloat(request.queryString().get("speed")[0]);
            return F.Promise.wrap(c.move3d(0, 0, 0, speed)).map(v -> ok(Json.newObject()));
        });
    }

    public static F.Promise<Result> command(Long id, String command) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return F.Promise.pure(notFound());

        if (drone.getStatus() != Drone.Status.MANUAL_CONTROL)
            return F.Promise.pure(forbidden(Json.toJson("you can only control a drone which is in manual control mode.")));



        if (!COMMANDS.containsKey(command))
            return F.Promise.pure(badRequest(Json.toJson("unknown command")));

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(drone);
        return COMMANDS.get(command).apply(commander,request());
    }

    public static F.Promise<Result> setManual(Long id) {
        return setMode(id, Drone.Status.MANUAL_CONTROL);
    }

    public static F.Promise<Result> setAutomatic(Long id) {
        return setMode(id, Drone.Status.AVAILABLE);
    }

    private static F.Promise<Result> setMode(Long id, Drone.Status status) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return F.Promise.pure(notFound());

        drone.setStatus(status);

        return F.Promise.pure(DroneController.update(id, JsonHelper.addRootElement(Json.toJson(drone), Drone.class).toString()));
    }

    public static Result links(Long id) {
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.ManualDroneController.links(id).absoluteURL(request())));
        links.add(new ControllerHelper.Link("manual", controllers.routes.ManualDroneController.setManual(id).absoluteURL(request())));
        links.add(new ControllerHelper.Link("automatic", controllers.routes.ManualDroneController.setAutomatic(id).absoluteURL(request())));
        links.addAll(COMMANDS.keySet().stream().map(key -> new ControllerHelper.Link(key, controllers.routes.ManualDroneController.command(id, key).absoluteURL(request()))).collect(Collectors.toList()));

        JsonNode node = JsonHelper.createJsonNode(Json.newObject(),links, Drone.class);
        return ok(node);
    }

}
