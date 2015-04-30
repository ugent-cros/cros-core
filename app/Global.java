import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.scheduler.AdvancedScheduler;
import drones.scheduler.Scheduler;
import drones.scheduler.SchedulerException;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.Scala;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import scala.Tuple2;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthias on 20/03/2015.
 */
public class Global extends GlobalSettings {

    private class ActionWrapper extends Action.Simple {
        public ActionWrapper(Action<?> action) {
            this.delegate = action;
        }

        @Override
        public F.Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
            F.Promise<Result> result = this.delegate.call(ctx);

            Http.Response response = ctx.response();
            response.setHeader("Access-Control-Allow-Origin", "*");
            return result;
        }
    }

    @Override
    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        ObjectNode result = Json.newObject();
        result.put("reason", t.getMessage());

        // Giant -hack- to add Origin control to error messages
        List<Tuple2<String, String>> list = new ArrayList<>();
        Tuple2<String, String> h = new Tuple2<>("Access-Control-Allow-Origin","*");
        list.add(h);
        Seq<Tuple2<String, String>> seq = Scala.toSeq(list);
        Result error = () -> Results.internalServerError(result).toScala().withHeaders(seq);
        return F.Promise.pure(error);
    }

    public void onStart(Application application) {
        super.onStart(application);
        try {
            Scheduler.start(AdvancedScheduler.class);
            Scheduler.addDrones();
        }catch(SchedulerException ex){
            Logger.error("Scheduler failed on start.",ex);
        }
    }

    @Override
    public void onStop(Application application) {
        super.onStop(application);
        try {
            Scheduler.stop();
        }catch(SchedulerException ex){
            Logger.error("Scheduler failed on stop.");
        }
    }

    public Action<?> onRequest(Http.Request request, java.lang.reflect.Method actionMethod) {
        return new ActionWrapper(super.onRequest(request, actionMethod));
    }

}
