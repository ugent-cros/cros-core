package controllers;

import models.Assignment;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Security;

import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

/**
 * Created by Benjamin on 5/03/2015.
 */
@Security.Authenticated(Secured.class)
public class AssignmentController {
    public static Result getAllAssignments() {
        return ok(JsonCast.toJson(Assignment.find.all(), Assignment.class));
    }

    public static Result createAssignment() {
        Form<Assignment> assignmentForm = Form.form(Assignment.class).bindFromRequest();

        if (assignmentForm.hasErrors())
            return badRequest(assignmentForm.errors().toString());

        Assignment assignment = assignmentForm.get();
        assignment.save();
        return ok(Json.toJson(assignment));
    }

    public static Result getAssignment(long id) {
        Assignment assignment = Assignment.find.byId(id);
        if(assignment == null)
            return badRequest("Requested assignment not found");

        return ok(Json.toJson(assignment));
    }

    public static Result deleteAssigment(long id) {
        Assignment assignment = Assignment.find.byId(id);

        if(assignment == null)
            return badRequest("Requested assignment not found");

        assignment.delete();
        return ok();
    }
}
