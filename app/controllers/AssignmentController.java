package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Assignment;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.ControllerHelper.Link;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Results.*;
import static play.mvc.Controller.request;

/**
 * Created by Benjamin on 5/03/2015.
 */
public class AssignmentController {

    @Authentication({User.Role.READONLY_ADMIN, User.Role.ADMIN})
    public static Result getAll() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

        ArrayNode array = objectMapper.createArrayNode();
        ObjectWriter writer = objectMapper.writerWithView(ControllerHelper.Summary.class);
        for(Assignment assignment : Assignment.FIND.all()) {
            try {
                ObjectNode assigmentNode = (ObjectNode) Json.parse(writer.writeValueAsString(assignment));

                List<Link> links = new ArrayList<>();
                links.add(new Link("details", controllers.routes.AssignmentController.get(assignment.getId()).url()));
                assigmentNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(assigmentNode);
            } catch (JsonProcessingException e) {
                play.Logger.error(e.getMessage(), e);
                return internalServerError();
            }
        }
        ObjectNode node = (ObjectNode) JsonHelper.addRootElement(array, Assignment.class);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new Link("self", controllers.routes.AssignmentController.getAll().url()));
        links.add(new Link("create", controllers.routes.AssignmentController.create().url()));
        node.put("links", (JsonNode) objectMapper.valueToTree(links));
        return ok(node);
    }

    @Authentication({User.Role.ADMIN, User.Role.USER})
    public static Result create() {
        JsonNode body = request().body().asJson();
        JsonNode strippedBody = JsonHelper.removeRootElement(body, Assignment.class);
        Form<Assignment> form = Form.form(Assignment.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errors().toString());

        User user = (User) Http.Context.current().args.get("user");
        if(user == null)
            return unauthorized();

        Assignment assignment = form.get();
        assignment.setCreator(user);

        assignment.save();
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(assignment), Assignment.class);
        return created(nodeWithRoot);
    }

    @Authentication({User.Role.READONLY_ADMIN, User.Role.ADMIN})
    public static Result get(long id) {
        Assignment assignment = Assignment.FIND.byId(id);
        if(assignment == null)
            return notFound("Requested assignment not found");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode) Json.toJson(assignment);
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.AssignmentController.get(assignment.getId()).url()));
        links.add(new ControllerHelper.Link("all", controllers.routes.AssignmentController.getAll().url()));
        links.add(new ControllerHelper.Link("delete", controllers.routes.AssignmentController.delete(assignment.getId()).url()));
        node.put("links", (JsonNode) mapper.valueToTree(links));

        JsonNode nodeWithRoot = JsonHelper.addRootElement(node, Assignment.class);
        return ok(nodeWithRoot);
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long id) {
        Assignment assignment = Assignment.FIND.byId(id);

        if(assignment == null)
            return notFound("Requested assignment not found");

        assignment.delete();
        return ok();
    }
}
