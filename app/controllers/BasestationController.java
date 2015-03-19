package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import models.Basestation;
import models.User;
import play.data.Form;
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

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getAll() {
        List<JsonHelper.Tuple> tuples = new ArrayList<>();
        for(Basestation basestation : Basestation.FIND.all()) {
            tuples.add(new JsonHelper.Tuple(basestation, new ControllerHelper.Link("self",
                    controllers.routes.BasestationController.get(basestation.getId()).url())));
        }

        // TODO: add links when available
        List<ControllerHelper.Link> links = new ArrayList<>();
        links.add(new ControllerHelper.Link("self", controllers.routes.BasestationController.getAll().url()));

        try {
            return ok(JsonHelper.createJsonNode(tuples, links, Basestation.class));
        } catch(JsonProcessingException ex) {
            play.Logger.error(ex.getMessage(), ex);
            return internalServerError();
        }
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
            play.Logger.error(ex.getMessage(), ex);
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
            play.Logger.error(ex.getMessage(), ex);
            return badRequest(ex.getMessage());
        }
        Form<Basestation> form = Form.form(Basestation.class).bind(strippedBody);

        if (form.hasErrors())
            return badRequest(form.errors().toString());

        Basestation updatedBaseStation = form.get();
        updatedBaseStation.setId(id);
        updatedBaseStation.getCheckpoint().update();
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
        links.add(new ControllerHelper.Link("self", controllers.routes.BasestationController.get(id).url()));
        return links;
    }
}
