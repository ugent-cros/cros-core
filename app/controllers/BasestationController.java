package controllers;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Basestation;
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
 * Created by Eveline on 8/03/2015.
 */
public class BasestationController {

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getAll() {
        ExpressionList<Basestation> exp = QueryHelper.buildQuery(Basestation.class, Basestation.FIND.where());

        List<JsonHelper.Tuple> tuples = exp.findList().stream().map(basestation -> new JsonHelper.Tuple(basestation, new ControllerHelper.Link("self",
                controllers.routes.BasestationController.get(basestation.getId()).absoluteURL(request())))).collect(Collectors.toList());

        // TODO: add links when available
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.BasestationController.getAll().absoluteURL(request())));
        links.add(new ControllerHelper.Link("total", controllers.routes.BasestationController.getTotal().absoluteURL(request())));

        try {
            JsonNode result = JsonHelper.createJsonNode(tuples, links, Basestation.class);
            String[] totalQuery = request().queryString().get("total");
            if (totalQuery != null && totalQuery.length == 1 && totalQuery[0].equals("true")) {
                ExpressionList<Basestation> countExpression = QueryHelper.buildQuery(Basestation.class, Basestation.FIND.where(), true);
                String root = Basestation.class.getAnnotation(JsonRootName.class).value();
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
        return ok(JsonHelper.addRootElement(Json.newObject().put("total", Basestation.FIND.findRowCount()), Basestation.class));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result get(long id) {
        Basestation basestation = Basestation.FIND.byId(id);

        if (basestation == null)
            return notFound();

        return ok(JsonHelper.createJsonNode(basestation, getAllLinks(id), Basestation.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result create() {
        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, Basestation.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            play.Logger.debug(ex.getMessage(), ex);
            return badRequest(ex.getMessage());
        }
        Form<Basestation> form = Form.form(Basestation.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errorsAsJson());

        Basestation basestation = form.get();
        basestation.save();
        return created(JsonHelper.createJsonNode(basestation, getAllLinks(basestation.getId()), Basestation.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result update(long id) {
        Basestation basestation = Basestation.FIND.byId(id);
        if (basestation == null)
            return notFound();

        JsonNode body = request().body().asJson();
        JsonNode strippedBody;
        try {
            strippedBody = JsonHelper.removeRootElement(body, Basestation.class, false);
        } catch(JsonHelper.InvalidJSONException ex) {
            play.Logger.debug(ex.getMessage(), ex);
            return badRequest(ex.getMessage());
        }
        Form<Basestation> form = Form.form(Basestation.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errors().toString());

        Basestation updatedBaseStation = form.get();
        updatedBaseStation.setId(id);
        updatedBaseStation.setVersion(basestation.getVersion());
        updatedBaseStation.update();
        return ok(JsonHelper.createJsonNode(updatedBaseStation, getAllLinks(updatedBaseStation.getId()),
                Basestation.class));
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long id) {
        Basestation basestation = Basestation.FIND.byId(id);
        if(basestation == null)
            return notFound("Requested basestation not found");

        basestation.delete();//cascading delete automatically
        return ok();
    }

    private static List<ControllerHelper.Link> getAllLinks(long id) {
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.BasestationController.get(id).absoluteURL(request())));
        return links;
    }
}
