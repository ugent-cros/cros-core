package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import drones.models.DroneCommander;
import drones.models.Fleet;
import drones.models.FlipType;
import models.Drone;
import models.User;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by matthias on 25/04/2015.
 */

@Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN, User.Role.USER})
public class ManualDroneController extends Controller {

    private static final Map<String,Function<DroneCommander,F.Promise<Result>>> COMMANDS;

    static {
        COMMANDS = new HashMap<>();
        COMMANDS.put("flipBack", (c) -> F.Promise.wrap(c.flip(FlipType.BACK)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flipFront", (c) -> F.Promise.wrap(c.flip(FlipType.FRONT)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flipLeft", (c) -> F.Promise.wrap(c.flip(FlipType.LEFT)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flipRight", (c) -> F.Promise.wrap(c.flip(FlipType.RIGHT)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setOutdoor", (c) -> F.Promise.wrap(c.setOutdoor(true)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setIndoor", (c) -> F.Promise.wrap(c.setOutdoor(false)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setHull", (c) -> F.Promise.wrap(c.setHull(true)).map(v -> ok(Json.newObject())));
        COMMANDS.put("setNoHull", (c) -> F.Promise.wrap(c.setHull(false)).map(v -> ok(Json.newObject())));
        COMMANDS.put("flatTrim", (c) -> F.Promise.wrap(c.flatTrim()).map(v -> ok(Json.newObject())));
        COMMANDS.put("takeOff", (c) -> F.Promise.wrap(c.takeOff()).map(v -> ok(Json.newObject())));
        COMMANDS.put("land", (c) -> F.Promise.wrap(c.land()).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveLeft", (c) -> F.Promise.wrap(c.move3d(0, -1, 0, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveRight", (c) -> F.Promise.wrap(c.move3d(0, 1, 0, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveUp", (c) -> F.Promise.wrap(c.move3d(0, 0, 1, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveDown", (c) -> F.Promise.wrap(c.move3d(0, 0, -1, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("center", (c) -> F.Promise.wrap(c.move3d(0, 0, 0, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveForward", (c) -> F.Promise.wrap(c.move3d(1, 0, 0, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("moveBackward", (c) -> F.Promise.wrap(c.move3d(-1, 0, 0, 0)).map(v -> ok(Json.newObject())));
        COMMANDS.put("rotateLeft", (c) -> F.Promise.wrap(c.move3d(0, 0, 0, -1)).map(v -> ok(Json.newObject())));
        COMMANDS.put("rotateRight", (c) -> F.Promise.wrap(c.move3d(0, 0, 0, 1)).map(v -> ok(Json.newObject())));
    }

    public static F.Promise<Result> command(Long id, String command) {
        Drone drone = Drone.FIND.byId(id);

        if (drone.getStatus() != Drone.Status.MANUAL_CONTROL)
            return F.Promise.pure(forbidden(Json.toJson("you can only control a drone which is in manual control mode.")));

        if (!COMMANDS.containsKey(command))
            return F.Promise.pure(badRequest(Json.toJson("unknown command")));

        DroneCommander commander = Fleet.getFleet().getCommanderForDrone(drone);
        return COMMANDS.get(command).apply(commander);
    }

    public static F.Promise<Result> setManual(Long id) {
        return setMode(Drone.FIND.byId(id), Drone.Status.MANUAL_CONTROL);
    }

    public static F.Promise<Result> setAutomatic(Long id) {
        return setMode(Drone.FIND.byId(id), Drone.Status.AVAILABLE);
    }

    private static F.Promise<Result> setMode(Drone drone, Drone.Status status) {
        if (drone.getStatus() == Drone.Status.CHARGING || drone.getStatus() == Drone.Status.DECOMMISSIONED ||
                drone.getStatus() == Drone.Status.EMERGENCY_LANDED || drone.getStatus() == Drone.Status.MISSING_DRIVER ||
                drone.getStatus() == Drone.Status.UNAVAILABLE || drone.getStatus() == Drone.Status.UNKNOWN ||
                drone.getStatus() == Drone.Status.UNREACHABLE)
            return F.Promise.pure(forbidden(Json.toJson("you cannot set control mode of a drone with status " + drone.getStatus().toString() + ".")));

        if (drone.getStatus() == status)
            return F.Promise.pure(ok(Json.toJson("drone was allready in mode " + status.toString() + ".")));

        drone.setStatus(status);
        drone.update();
        return F.Promise.pure(ok(Json.toJson("drone mode updated to " + status.toString() + ".")));
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
