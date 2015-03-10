package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.models.Drone;
import drones.models.Fleet;
import play.*;
import play.libs.F;
import play.libs.Json;
import play.mvc.*;

import scala.util.parsing.json.JSON;
import scala.util.parsing.json.JSONObject$;
import views.html.*;

public class Application extends Controller {

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public static F.Promise<Result> initDrone() {
        Drone d = Fleet.getFleet().createBepop("bepop", "localhost", true);
        return F.Promise.wrap(d.init()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }

    public static F.Promise<Result> getBatteryPercentage(){
        Drone d = Fleet.getFleet().getDrone("bepop");
        return F.Promise.wrap(d.getBatteryPercentage()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("batteryPercentage", v);
            return ok(result);
        });
    }

    public static F.Promise<Result> getLocation(){
        Drone d = Fleet.getFleet().getDrone("bepop");
        return F.Promise.wrap(d.getLocation()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("long", v.getLongtitude());
            result.put("lat", v.getLatitude());
            result.put("heigth", v.getHeigth());
            return ok(result);
        });
    }

    public static F.Promise<Result> takeOff(){
        Drone d = Fleet.getFleet().getDrone("bepop");
        return F.Promise.wrap(d.takeOff()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }

    public static F.Promise<Result> land(){
        Drone d = Fleet.getFleet().getDrone("bepop");
        return F.Promise.wrap(d.land()).map(v -> {
            ObjectNode result = Json.newObject();
            result.put("status", "ok");
            return ok(result);
        });
    }


}
