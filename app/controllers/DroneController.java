package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Drone;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ControllerHelper;
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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode array = objectMapper.createArrayNode();
        rootNode.put("drone", array);

        for(Drone d : Drone.find.all()) {
            ObjectNode droneNode = null;
            try {
                droneNode = (ObjectNode) Json.parse(objectMapper.writerWithView(ControllerHelper.Summary.class).writeValueAsString(d));

                List<ControllerHelper.Link> links = new ArrayList<>();
                links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.getAll().url()));
                links.add(new ControllerHelper.Link("all", controllers.routes.DroneController.getAll().url()));
                links.add(new ControllerHelper.Link("add", controllers.routes.DroneController.add().url()));
                links.add(new ControllerHelper.Link("delete", controllers.routes.DroneController.delete(d.id).url()));
                links.add(new ControllerHelper.Link("update", controllers.routes.DroneController.update(d.id).url()));
                links.add(new ControllerHelper.Link("details", controllers.routes.DroneController.get(d.id).url()));
                droneNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(droneNode);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return internalServerError();
            }
        }
        return ok(rootNode);
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result get(long i) {
        Drone d = Drone.find.byId(i);

        if (d == null)
            return notFound();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode) Json.toJson(d);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.get(d.id).url()));
        links.add(new ControllerHelper.Link("all", controllers.routes.DroneController.getAll().url()));
        links.add(new ControllerHelper.Link("add", controllers.routes.DroneController.add().url()));
        links.add(new ControllerHelper.Link("delete", controllers.routes.DroneController.delete(d.id).url()));
        links.add(new ControllerHelper.Link("update", controllers.routes.DroneController.update(d.id).url()));
        links.add(new ControllerHelper.Link("details", controllers.routes.DroneController.get(d.id).url()));
        ((ObjectNode) node.get("drone")).put("links", (JsonNode) mapper.valueToTree(links));

        return ok(node);
    }

    @Authentication({User.Role.ADMIN})
    public static Result add() {
        JsonNode node = request().body().asJson();
        Form<Drone> droneForm = Form.form(Drone.class).bind(node);

        if (droneForm.hasErrors())
            return badRequest(droneForm.errors().toString());

        Drone drone = droneForm.get();
        drone.save();
        return get(drone.id);
    }

    @Authentication({User.Role.ADMIN})
    public static Result update(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        Form<Drone> f = Form.form(Drone.class).bind(request().body().asJson());

        if (f.hasErrors())
            return badRequest(f.errors().toString());

        Drone updatedDrone = f.get();
        updatedDrone.id = drone.id;
        updatedDrone.update();
        return get(updatedDrone.id);
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result location(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.location()));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result testConnection(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.testConnection()));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result battery(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.getBatteryStatus()));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result cameraCapture(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.getCameraCapture()));
    }

    @Authentication({User.Role.ADMIN})
    public static Result emergency(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        drone.emergency();
        return ok();
    }

    @Authentication({User.Role.ADMIN})
    public static Result deleteAll() {
        Drone.find.all().forEach(d -> d.delete());
        return ok();
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long i) {
        Drone d = Drone.find.byId(i);
        if (d == null)
            return notFound();

        d.delete();
        return ok();
    }



}
