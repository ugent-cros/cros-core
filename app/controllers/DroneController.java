package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Drone;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ControllerHelper;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Results.*;

/**
 * Created by matthias on 19/02/2015.
 */

@Security.Authenticated(Secured.class)
public class DroneController {

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
                links.add(new ControllerHelper.Link("self", routes.DroneController.getAll().url()));
                links.add(new ControllerHelper.Link("all", routes.DroneController.getAll().url()));
                links.add(new ControllerHelper.Link("add", routes.DroneController.add().url()));
                links.add(new ControllerHelper.Link("delete", routes.DroneController.delete(d.id).url()));
                links.add(new ControllerHelper.Link("update", routes.DroneController.update(d.id).url()));
                links.add(new ControllerHelper.Link("details", routes.DroneController.get(d.id).url()));
                droneNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(droneNode);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return internalServerError();
            }
        }
        return ok(rootNode);
    }

    public static Result get(long i) {
        Drone d = Drone.find.byId(i);

        if (d == null)
            return notFound();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode) Json.toJson(d);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", routes.DroneController.get(d.id).url()));
        links.add(new ControllerHelper.Link("all", routes.DroneController.getAll().url()));
        links.add(new ControllerHelper.Link("add", routes.DroneController.add().url()));
        links.add(new ControllerHelper.Link("delete", routes.DroneController.delete(d.id).url()));
        links.add(new ControllerHelper.Link("update", routes.DroneController.update(d.id).url()));
        links.add(new ControllerHelper.Link("details", routes.DroneController.get(d.id).url()));
        ((ObjectNode) node.get("drone")).put("links", (JsonNode) mapper.valueToTree(links));

        return ok(node);
    }

    public static Result add() {
        Form<Drone> droneForm = Form.form(Drone.class).bindFromRequest();

        if (droneForm.hasErrors())
            return badRequest(droneForm.errors().toString());

        Drone drone = droneForm.get();
        drone.save();
        return get(drone.id);
    }

    public static Result update(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        Form<Drone> f = Form.form(Drone.class);
        Form<Drone> newData = Form.form(Drone.class).bindFromRequest();
        f.bind(newData.data());

        if (f.hasErrors())
            return badRequest(f.errors().toString());

        Drone updatedDrone = f.get();
        drone.update(updatedDrone);
        return get(updatedDrone.id);
    }

    public static Result location(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.location()));
    }

    public static Result testConnection(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.testConnection()));
    }

    public static Result battery(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.getBatteryStatus()));
    }

    public static Result cameraCapture(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        return ok(Json.toJson(drone.getCameraCapture()));
    }

    public static Result emergency(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return notFound();

        drone.emergency();
        return ok();
    }

    public static Result deleteAll() {
        Drone.find.all().forEach(d -> d.delete());
        return ok();
    }

    public static Result delete(long i) {
        Drone d = Drone.find.byId(i);
        if (d == null)
            return notFound();

        d.delete();
        return ok();
    }



}
