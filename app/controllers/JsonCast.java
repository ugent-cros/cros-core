package controllers;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import play.libs.Json;
import play.mvc.Http;

import java.util.Collection;

/**
 * Created by matthias on 20/02/2015.
 */
public class JsonCast {


    public static <T> T cast(Http.Request request, Class<T> type) {
        JsonNode n = request.body().asJson();
        try {
            return Json.fromJson(n, type);
        } catch (Exception ex) {
            ex.printStackTrace();
            play.Logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    public static String toJson(Collection o, Class c) {
        ObjectMapper mapper = new ObjectMapper();
        JsonRootName name = (JsonRootName) c.getAnnotation(JsonRootName.class);
        ObjectWriter writer = mapper.writer().withRootName(name.value());
        try {
            return writer.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            play.Logger.error(ex.getMessage(), ex);
            return null;
        }
    }

}
