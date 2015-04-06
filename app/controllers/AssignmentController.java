package controllers;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Assignment;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import utilities.ControllerHelper;
import utilities.JsonHelper;
import utilities.QueryHelper;
import utilities.annotations.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static play.mvc.Controller.request;
import static play.mvc.Results.*;

/**
 * Created by Benjamin on 5/03/2015.
 */
public class AssignmentController {

    @Authentication({User.Role.READONLY_ADMIN, User.Role.ADMIN})
    public static Result getAll() {
        ExpressionList<Assignment> exp = QueryHelper.buildQuery(Assignment.class, Assignment.FIND.where());

        List<JsonHelper.Tuple> tuples = exp.findList().stream().map(assignment -> new JsonHelper.Tuple(assignment, new ControllerHelper.Link("self",
                controllers.routes.AssignmentController.get(assignment.getId()).url()))).collect(Collectors.toList());

        // TODO: add links when available
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.AssignmentController.getAll().url()));
        links.add(new ControllerHelper.Link("total", controllers.routes.AssignmentController.getTotal().url()));

        try {
            JsonNode result = JsonHelper.createJsonNode(tuples, links, Assignment.class);
            String[] totalQuery = request().queryString().get("total");
            if (totalQuery != null && totalQuery.length == 1 && totalQuery[0].equals("true")) {
                ExpressionList<Assignment> countExpression = QueryHelper.buildQuery(Assignment.class, Assignment.FIND.where(), true);
                String root = Assignment.class.getAnnotation(JsonRootName.class).value();
                ((ObjectNode) result.get(root)).put("total",countExpression.findRowCount());
            }
            return ok(result);
        } catch(JsonProcessingException ex) {
            play.Logger.error(ex.getMessage(), ex);
            return internalServerError();
        }
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getTotal() {
        return ok(JsonHelper.addRootElement(Json.newObject().put("total", Assignment.FIND.findRowCount()), Assignment.class));
    }

    @Authentication({User.Role.READONLY_ADMIN, User.Role.ADMIN})
    public static Result get(long id) {
        Assignment assignment = Assignment.FIND.byId(id);

        if (assignment == null)
            return notFound();

        // TODO: summary view of user instead of complete user informartion in json
        return ok(JsonHelper.createJsonNode(assignment, getAllLinks(id), Assignment.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.USER})
    public static Result create() {
        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, Assignment.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            play.Logger.debug(ex.getMessage(), ex);
            return badRequest(ex.getMessage());
        }
        Form<Assignment> form = Form.form(Assignment.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errorsAsJson());

        User user = SecurityController.getUser();
        if(user == null)
            return unauthorized();

        Assignment assignment = form.get();
        assignment.setCreator(user);
        assignment.save();
        return created(JsonHelper.createJsonNode(assignment, getAllLinks(assignment.getId()), Assignment.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long id) {
        Assignment assignment = Assignment.FIND.byId(id);

        if(assignment == null)
            return notFound("Requested assignment not found");

        assignment.delete();
        return ok();
    }

    private static List<ControllerHelper.Link> getAllLinks(long id) {
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.AssignmentController.get(id).url()));
        return links;
    }
}
