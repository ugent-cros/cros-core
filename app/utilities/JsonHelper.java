package utilities;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

/**
 * Created by Benjamin on 9/03/2015.
 */
public class JsonHelper {

    public static JsonNode removeRootElement(JsonNode node, Class clazz) {
        JsonRootName annotation = (JsonRootName) clazz.getAnnotation(JsonRootName.class);
        String rootElement = annotation.value();
        return node.get(rootElement);
    }

    public static JsonNode removeRootElement(String jsonString, Class clazz) {
        JsonNode node = Json.parse(jsonString);
        return removeRootElement(node, clazz);
    }

    public static JsonNode addRootElement(JsonNode node, Class clazz) {
        JsonRootName annotation = (JsonRootName) clazz.getAnnotation(JsonRootName.class);
        String rootElement = annotation.value();
        ObjectNode nodeWithRoot = Json.newObject();
        nodeWithRoot.put(rootElement, node);
        return nodeWithRoot;
    }
}
