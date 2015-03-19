package utilities;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.databind.JsonNode;
import play.db.ebean.Model;
import play.libs.Json;

import java.util.Map;

import static play.mvc.Controller.request;

/**
 * Created by matthias on 18/03/2015.
 */
public class QueryHelper {

    public static <T extends Model> ExpressionList<T> buildQuery(Class<T> clazz , ExpressionList<T> exp) {
        JsonNode model;
        try {
            model = Json.toJson(clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            play.Logger.error("the provided class is not accessible or cannot be instantiated");
            return null;
        }

        int pageSize = 0;
        if (request().queryString().containsKey("pageSize"))
            pageSize = Integer.parseInt(request().queryString().get("pageSize")[0]);

        boolean asc = true;
        if (request().queryString().containsKey("order"))
            asc = !"desc".equals(request().queryString().get("order")[0]);


        for (Map.Entry<String,String[]> e : request().queryString().entrySet()) {
            String key = e.getKey();
            switch (key) {
                case "orderBy" :
                    if (asc)
                        exp.order().asc(e.getValue()[0]);
                    else
                        exp.order().desc(e.getValue()[0]);
                    break;
                case "page" :
                    if (pageSize == 0)
                        break;
                    exp.setFirstRow(pageSize * Integer.parseInt(e.getValue()[0])).setMaxRows(pageSize);
                    break;
                default :
                    JsonNode result = model.findValue(key);
                    if (result != null)
                        exp = exp.contains(key, e.getValue()[0]);
            }
        }

        return exp;
    }
}
