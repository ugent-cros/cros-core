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
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.ControllerHelper.Link;
import utilities.JsonHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.List;

import static play.mvc.Results.*;

/**
 * Created by Benjamin on 5/03/2015.
 */
public class AssignmentController {
    public static Result getAll() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);

        ArrayNode array = objectMapper.createArrayNode();
        ObjectWriter writer = objectMapper.writerWithView(ControllerHelper.Summary.class);
        for(Assignment assignment : Assignment.find.all()) {
            try {
                ObjectNode assigmentNode = (ObjectNode) Json.parse(writer.writeValueAsString(assignment));

                List<Link> links = new ArrayList<>();
                links.add(new Link("self", controllers.routes.AssignmentController.getAll().url()));
                links.add(new Link("details", controllers.routes.AssignmentController.get(assignment.id).url()));
                assigmentNode.put("links", (JsonNode) objectMapper.valueToTree(links));
                array.add(assigmentNode);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return internalServerError();
            }
        }
        JsonNode nodeWithRoot = JsonHelper.addRootElement(array, Assignment.class);
        return ok(nodeWithRoot);
    }

    @Authentication(User.Role.ADMIN)
    public static Result create() {
        JsonNode body = Controller.request().body().asJson();
        JsonNode strippedBody = JsonHelper.removeRootElement(body, Assignment.class);
        Form<Assignment> form = Form.form(Assignment.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errors().toString());

        User user = (User) Http.Context.current().args.get("user");
        if(user == null)
            return unauthorized();

        Assignment assignment = form.get();
        assignment.creator = user;

        assignment.save();
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(assignment), Assignment.class);
        return ok(nodeWithRoot);
    }

    public static Result get(long id) {
        Assignment assignment = Assignment.find.byId(id);
        if(assignment == null)
            return notFound("Requested assignment not found");

        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(assignment), Assignment.class);
        return ok(nodeWithRoot);
    }

    public static Result delete(long id) {
        Assignment assignment = Assignment.find.byId(id);

        if(assignment == null)
            return badRequest("Requested assignment not found");

        assignment.delete();
        return ok();
    }
}
