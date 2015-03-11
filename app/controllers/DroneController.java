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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

        ArrayNode array = objectMapper.createArrayNode();
        for(Drone drone : Drone.FIND.all()) {
            try {
                ObjectNode droneNode = (ObjectNode) Json.parse(objectMapper.writerWithView(ControllerHelper.Summary.class).writeValueAsString(drone));

                List<ControllerHelper.Link> links = new ArrayList<>();
                links.add(new ControllerHelper.Link("details", controllers.routes.DroneController.get(drone.getId()).url()));
                droneNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(droneNode);
            } catch (JsonProcessingException e) {
                play.Logger.error(e.getMessage(), e);
                return internalServerError();
            }
        }

        ObjectNode node = (ObjectNode) JsonHelper.addRootElement(array, Drone.class);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.getAll().url()));
        links.add(new ControllerHelper.Link("create", controllers.routes.DroneController.create().url()));
        node.put("links", (JsonNode) objectMapper.valueToTree(links));

        return ok(node);
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result get(long i) {
        Drone drone = Drone.FIND.byId(i);

        if (drone == null)
            return notFound();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode) Json.toJson(drone);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.get(drone.getId()).url()));
        links.add(new ControllerHelper.Link("all", controllers.routes.DroneController.getAll().url()));
        links.add(new ControllerHelper.Link("delete", controllers.routes.DroneController.delete(drone.getId()).url()));
        links.add(new ControllerHelper.Link("update", controllers.routes.DroneController.update(drone.getId()).url()));
        node.put("links", (JsonNode) mapper.valueToTree(links));

        return ok(JsonHelper.addRootElement(node, Drone.class));
    }

    @Authentication({User.Role.ADMIN})
    @BodyParser.Of(BodyParser.Json.class)
    public static Result create() {
        JsonNode node = JsonHelper.removeRootElement(request().body().asJson(), Drone.class);
        Form<Drone> droneForm = Form.form(Drone.class).bind(node);

        if (droneForm.hasErrors())
            return badRequest(droneForm.errorsAsJson());

        Drone drone = droneForm.get();
        drone.save();

        ObjectNode responseNode = (ObjectNode) Json.toJson(drone);

        // Add links to result
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.DroneController.get(drone.getId()).url()));
        links.add(new ControllerHelper.Link("all", controllers.routes.DroneController.getAll().url()));
        links.add(new ControllerHelper.Link("delete", controllers.routes.DroneController.delete(drone.getId()).url()));
        links.add(new ControllerHelper.Link("update", controllers.routes.DroneController.update(drone.getId()).url()));
        responseNode.put("links", (JsonNode) new ObjectMapper().valueToTree(links));

        return created(JsonHelper.addRootElement(responseNode, Drone.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result update(Long id) {
        Drone drone = Drone.FIND.byId(id);
        if (drone == null)
            return notFound();

        JsonNode node = JsonHelper.removeRootElement(request().body().asJson(), Drone.class);
        Form<Drone> f = Form.form(Drone.class).bind(node);

        if (f.hasErrors())
            return badRequest(f.errors().toString());

        Drone updatedDrone = f.get();
        updatedDrone.setId(drone.getId());
        updatedDrone.update();
        return get(updatedDrone.getId());
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
        node.put("battery", drone.getBatteryStatus());
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
