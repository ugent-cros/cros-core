package controllers;

import models.Basestation;
import models.Checkpoint;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import utilities.annotations.Authentication;

import java.util.List;

import static play.mvc.Results.*;

/**
 * Created by Eveline on 8/03/2015.
 */
public class BasestationController {

    private static Form<Basestation> form = Form.form(Basestation.class);

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result getAll() {
        List<Basestation> all = Basestation.find.all();
        return ok(Json.toJson(all));
    }

    @Authentication({User.Role.ADMIN})
    public static Result add() {
        Form<Basestation> filledForm = form.bindFromRequest();

        //TODO check input

        if (filledForm.hasErrors()){
            return badRequest(filledForm.errorsAsJson());
        }
        Checkpoint cp = new Checkpoint(Double.valueOf(filledForm.data().get("longitude")),
                                        Double.valueOf(filledForm.data().get("lattitude")),
                                        Double.valueOf(filledForm.data().get("altitude")));
        cp.save();
        Basestation basestation = filledForm.get();
        basestation.checkpoint = cp;
        basestation.save();
        return created(Json.toJson(basestation));
    }

    @Authentication({User.Role.ADMIN, User.Role.READONLY_ADMIN})
    public static Result get(long id) {
        Basestation basestation = Basestation.find.byId(id);

        if(basestation == null){
            return badRequest();
        }

        return ok(Json.toJson(basestation));
    }

    @Authentication({User.Role.ADMIN})
    public static Result update(long id) {
        if(Basestation.find.byId(id) == null){
            return notFound("No such basestation");
        }
        Form<Basestation> filledForm = form.bindFromRequest();

        //TODO check input

        if (filledForm.hasErrors()){
            return badRequest(filledForm.errorsAsJson());
        }

        //update basestation
        long cp_id = Long.valueOf(filledForm.data().get("checkpoint_id"));
        Checkpoint cp = Checkpoint.find.byId(cp_id);
        Basestation basestation = filledForm.get();
        basestation.checkpoint = cp;
        basestation.update(id);
        return ok();
    }

    @Authentication({User.Role.ADMIN})
    public static Result delete(long id) {
        Basestation basestation = Basestation.find.byId(id);
        if(basestation == null){
            return badRequest();
        }

        basestation.delete();//cascading delete automatically
        return ok();
    }
}
