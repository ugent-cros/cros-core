package utilities;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

/**
 * Created by Benjamin on 9/03/2015.
 */
public class JsonHelper {

    public static JsonNode removeRootElement(JsonNode node, Class clazz) throws InvalidJSONException {
        JsonRootName annotation = (JsonRootName) clazz.getAnnotation(JsonRootName.class);
        String rootElement = annotation.value();
        JsonNode rootNode = node.get(rootElement);
        if(rootNode == null)
            throw new InvalidJSONException("Invalid json: no such root element (" + annotation.value() + ")");
        return rootNode;
    }

    public static JsonNode removeRootElement(String jsonString, Class clazz) throws InvalidJSONException {
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

    public static class InvalidJSONException extends Exception {

        public InvalidJSONException(String message) {
            super(message);
        }

        public InvalidJSONException() { }
    }
}
