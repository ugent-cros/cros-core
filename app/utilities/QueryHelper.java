package utilities;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.databind.JsonNode;
import play.db.ebean.Model;
import play.libs.Json;

import static play.mvc.Controller.request;

/**
 * Created by matthias on 18/03/2015.
 */
public class QueryHelper {

    private static final String PAGE_SIZE = "pageSize";
    private static final String ORDER = "order";

    public static <T extends Model> ExpressionList<T> buildQuery(Class<T> clazz , ExpressionList<T> exp) {
        return buildQuery(clazz,exp,false);
    }

    public static <T extends Model> ExpressionList<T> buildQuery(Class<T> clazz , ExpressionList<T> exp, boolean ignorePage) {
        JsonNode model;
        try {
            model = Json.toJson(clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            play.Logger.error("the provided class is not accessible or cannot be instantiated", e);
            return null;
        }

        int pageSize = getPageSize();
        boolean asc = isAsc();

        request().queryString().entrySet().stream().forEach(e -> {
            switch (e.getKey()) {
                case "orderBy" :
                    exp.order().asc(e.getValue()[0]);
                    if (!asc)
                        exp.order().reverse();
                    break;
                case "page" :
                    if (pageSize > 0 && !ignorePage)
                        exp.setFirstRow(pageSize * Integer.parseInt(e.getValue()[0])).setMaxRows(pageSize);
                    break;
                case "total" :
                    break;
                default :
                    if (model.findValue(e.getKey()) != null)
                        exp.contains(e.getKey(), e.getValue()[0]);
            }
        });

        return exp;
    }

    private static int getPageSize() {
        if (request().queryString().containsKey(PAGE_SIZE))
            return Integer.parseInt(request().queryString().get(PAGE_SIZE)[0]);
        return -1;
    }

    private static boolean isAsc() {
        if (request().queryString().containsKey(ORDER))
            return !"desc".equals(request().queryString().get(ORDER)[0]);
        return true;
    }
}
