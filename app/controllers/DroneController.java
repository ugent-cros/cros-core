package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Drone;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Controller.request;
import static play.mvc.Results.*;

/**
 * Created by matthias on 19/02/2015.
 */

public class DroneController {

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getAll() {
        List<JsonHelper.Tuple> tuples = new ArrayList<>();
        for(Drone drone : Drone.FIND.all()) {
            tuples.add(new JsonHelper.Tuple(drone, new ControllerHelper.Link("self",
                    controllers.routes.DroneController.get(drone.getId()).url())));
        }

        // TODO: uncomment and add links when available
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.getAll().url()));
        //links.add(new ControllerHelper.Link("search", );

        try {
            return ok(JsonHelper.createJsonNode(tuples, links, Drone.class));
        } catch(JsonProcessingException ex) {
            play.Logger.error(ex.getMessage(), ex);
            return internalServerError();
        }
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result get(long id) {
        Drone drone = Drone.FIND.byId(id);

        if (drone == null)
            return notFound();

        // TODO: uncomment and add links when available
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.get(id).url()));
        links.add(new ControllerHelper.Link("connection", controllers.routes.DroneController.testConnection(id).url()));
        links.add(new ControllerHelper.Link("battery", controllers.routes.DroneController.battery(id).url()));
        links.add(new ControllerHelper.Link("cameraCapture", controllers.routes.DroneController.cameraCapture(id).url()));
        links.add(new ControllerHelper.Link("emergency", controllers.routes.DroneController.emergency(id).url()));
        links.add(new ControllerHelper.Link("location", controllers.routes.DroneController.location(id).url()));
        //links.add(new ControllerHelper.Link("altitude", );
        //links.add(new ControllerHelper.Link("speed", );

        return ok(JsonHelper.createJsonNode(drone, links, Drone.class));
    }

    @Authentication({User.Role.ADMIN})
    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {
        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, Drone.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            return badRequest(ex.getMessage());
        }
        Form<Drone> droneForm = Form.form(Drone.class).bind(strippedBody);

        if (droneForm.hasErrors())
            return badRequest(droneForm.errorsAsJson());

        Drone drone = droneForm.get();
        drone.save();

        return created(JsonHelper.createJsonNode(drone, Drone.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result update(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, Drone.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            return badRequest(ex.getMessage());
        }
        Form<Drone> droneForm = Form.form(Drone.class).bind(strippedBody);

        if (droneForm.hasErrors())
            return badRequest(droneForm.errors().toString());

        Drone updatedDrone = droneForm.get();
        updatedDrone.setId(drone.getId());
        updatedDrone.update();
        return created(JsonHelper.createJsonNode(updatedDrone, Drone.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result location(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        JsonNode node = JsonHelper.addRootElement(Json.toJson(drone.getLocation()), Drone.Location.class);
        return ok(node);
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result testConnection(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        ObjectNode node = Json.newObject();
        node.put("connection", drone.testConnection());
        return ok(JsonHelper.addRootElement(node, Drone.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result battery(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        ObjectNode node = Json.newObject();
        node.put("battery", drone.getBatteryPercentage());
        return ok(JsonHelper.addRootElement(node, Drone.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result cameraCapture(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        ObjectNode node = Json.newObject();
        node.put("cameraCapture", drone.getCameraCapture());
        return ok(JsonHelper.addRootElement(node, Drone.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result emergency(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        drone.emergency();
        return ok();
    }

    @Authentication({User.Role.ADMIN})
    public static Result deleteAll() {
        Drone.FIND.all().forEach(d -> d.delete());
        return ok();
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long i) {
        Drone d = Drone.FIND.byId(i);
        if (d == null)
            return notFound();

        d.delete();
        return ok();
    }



}
