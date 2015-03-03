package controllers;

import models.Drone;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

import static play.mvc.Http.Context.Implicit.request;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;

/**
 * Created by matthias on 19/02/2015.
 */

@Security.Authenticated(Secured.class)
public class DroneController {

    public static Result getAll() {
        return ok(JsonCast.toJson(Drone.find.all(),Drone.class));
    }

    public static Result get(long i) {
        Drone d = Drone.find.byId(i);

        if (d == null)
            return badRequest();

        return ok(Json.toJson(d));
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
        Form<Drone> droneForm = Form.form(Drone.class).bindFromRequest();

        if (droneForm.hasErrors())
            return badRequest(droneForm.errors().toString());

        Drone original = Drone.find.byId(id);
        original.update(droneForm.get());
        return get(original.id);
    }

    public static Result location(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return badRequest();

        return ok(Json.toJson(drone.location()));
    }

    public static Result testConnection(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return badRequest();

        return ok(Json.toJson(drone.testConnection()));
    }

    public static Result battery(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return badRequest();

        return ok(Json.toJson(drone.getBatteryStatus()));
    }

    public static Result cameraCapture(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return badRequest();

        return ok(Json.toJson(drone.getCameraCapture()));
    }

    public static Result emergency(Long id) {
        Drone drone = Drone.find.byId(id);
        if (drone == null)
            return badRequest();

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
            return badRequest();

        d.delete();
        return ok();
    }



}
