package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Assignment;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utilities.JsonHelper;

import java.util.List;

import static play.mvc.Results.*;

/**
 * Created by Benjamin on 5/03/2015.
 */
@Security.Authenticated(Secured.class)
public class AssignmentController {
    public static Result getAllAssignments() {
        List<Assignment> all = Assignment.find.all();
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(all), Assignment.class);
        return ok(nodeWithRoot);
    }

    public static Result createAssignment() {
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

    public static Result getAssignment(long id) {
        Assignment assignment = Assignment.find.byId(id);
        if(assignment == null)
            return notFound("Requested assignment not found");

        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(assignment), Assignment.class);
        return ok(nodeWithRoot);
    }

    public static Result deleteAssigment(long id) {
        Assignment assignment = Assignment.find.byId(id);

        if(assignment == null)
            return badRequest("Requested assignment not found");

        assignment.delete();
        return ok();
    }
}
