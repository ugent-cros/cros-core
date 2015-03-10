package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Basestation;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Controller.request;
import static play.mvc.Results.*;
/**
 * Created by Eveline on 8/03/2015.
 */
public class BasestationController {

    private static Form<Basestation> form = Form.form(Basestation.class);

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getAll() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

        ArrayNode array = objectMapper.createArrayNode();
        ObjectWriter writer = objectMapper.writerWithView(ControllerHelper.Summary.class);
        for(Basestation basestation : Basestation.find.all()) {
            try {
                ObjectNode basestationNode = (ObjectNode) Json.parse(writer.writeValueAsString(basestation));

                List<ControllerHelper.Link> links = new ArrayList<>();
                links.add(new ControllerHelper.Link("details", controllers.routes.BasestationController.get(basestation.id).url()));
                basestationNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(basestationNode);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return internalServerError();
            }
        }
        ObjectNode node = (ObjectNode) JsonHelper.addRootElement(array, Basestation.class);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.BasestationController.getAll().url()));
        links.add(new ControllerHelper.Link("create", controllers.routes.BasestationController.create().url()));
        node.put("links", (JsonNode) objectMapper.valueToTree(links));
        return ok(node);
    }

    @Authentication({User.Role.ADMIN})
    public static Result create() {
        JsonNode body = request().body().asJson();
        JsonNode strippedBody = JsonHelper.removeRootElement(body, Basestation.class);
        Form<Basestation> form = Form.form(Basestation.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errors().toString());

        Basestation basestation = form.get();
        basestation.save();
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(basestation), Basestation.class);
        return created(nodeWithRoot);
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result get(long id) {
        Basestation basestation = Basestation.find.byId(id);

        if(basestation == null)
            return notFound("Requested basestation not found");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode) Json.toJson(basestation);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.BasestationController.get(basestation.id).url()));
        links.add(new ControllerHelper.Link("all", controllers.routes.BasestationController.getAll().url()));
        links.add(new ControllerHelper.Link("update", controllers.routes.BasestationController.update(basestation.id).url()));
        links.add(new ControllerHelper.Link("delete", controllers.routes.BasestationController.delete(basestation.id).url()));
        node.put("links", (JsonNode) mapper.valueToTree(links));

        JsonNode nodeWithRoot = JsonHelper.addRootElement(node, Basestation.class);
        return ok(nodeWithRoot);
    }

    @Authentication({User.Role.ADMIN})
    public static Result update(long id) {
        Basestation basestation = Basestation.find.byId(id);
        if (basestation == null)
            return notFound();

        JsonNode node = JsonHelper.removeRootElement(request().body().asJson(), Basestation.class);
        Form<Basestation> form = Form.form(Basestation.class).bind(node);

        if (form.hasErrors())
            return badRequest(form.errors().toString());

        Basestation updatedBaseStation = form.get();
        updatedBaseStation.id = id;
        updatedBaseStation.checkpoint.update();
        updatedBaseStation.update();
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(updatedBaseStation), Basestation.class);
        return ok(nodeWithRoot);
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long id) {
        Basestation basestation = Basestation.find.byId(id);
        if(basestation == null)
            return notFound("Requested basestation not found");

        basestation.delete();//cascading delete automatically
        return ok();
    }
}
