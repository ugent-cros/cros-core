package utilities;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import utilities.ControllerHelper.Link;

import java.util.List;

/**
 * Created by Benjamin on 9/03/2015.
 */
public class JsonHelper {

    private static final String LINKS = "links";
    private static final String DEFAULT_ROOT_ELEMENT = "resource";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonNode createJsonNode(ObjectNode node, List<Link> links, Class clazz) {
        if(links != null && !links.isEmpty())
            node.put(LINKS, linksToNode(links));
        return addRootElement(node, clazz);
    }

    public static JsonNode createJsonNode(Object object, List<Link> links, Class clazz) {
        ObjectNode node = (ObjectNode) Json.toJson(object);
        return createJsonNode(node, links, clazz);
    }

    public static JsonNode createJsonNode(Object object, Class clazz) {
        return createJsonNode(object, null, clazz);
    }

    public static JsonNode createJsonNode(List<Tuple> objectsWithLinks, List<Link> links, Class clazz)
            throws JsonProcessingException {
        MAPPER.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        ArrayNode array = MAPPER.createArrayNode();
        for(Tuple objectWithLink : objectsWithLinks) {
            ObjectNode node =
                    (ObjectNode) Json.parse(MAPPER.writerWithView(Summary.class).writeValueAsString(objectWithLink.getObject()));
            if(objectWithLink.link != null)
                node.put(LINKS, addlinkToNode(MAPPER.createObjectNode(), objectWithLink.getLink()));
            array.add(node);
        }
        ObjectNode nodeWithArray = Json.newObject();
        nodeWithArray.put(DEFAULT_ROOT_ELEMENT, array);
        MAPPER.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);
        return createJsonNode(nodeWithArray, links, clazz);
    }

    public static JsonNode removeRootElement(JsonNode node, Class clazz, boolean isList) throws InvalidJSONException {
        JsonRootName annotation = (JsonRootName) clazz.getAnnotation(JsonRootName.class);
        String rootElement = annotation.value();
        JsonNode rootNode = isList ? node.get(rootElement).get(DEFAULT_ROOT_ELEMENT) : node.get(rootElement);
        if(rootNode == null)
            throw new InvalidJSONException("Invalid json: no such element (" +
                    (isList ? DEFAULT_ROOT_ELEMENT : annotation.value()) + ")");
        return rootNode;
    }

    public static JsonNode removeRootElement(String jsonString, Class clazz, boolean isList) throws InvalidJSONException {
        JsonNode node = Json.parse(jsonString);
        return removeRootElement(node, clazz, isList);
    }

    public static JsonNode addRootElement(JsonNode node, Class clazz) {
        JsonRootName annotation = (JsonRootName) clazz.getAnnotation(JsonRootName.class);
        String rootElement = annotation.value();
        ObjectNode nodeWithRoot = Json.newObject();
        nodeWithRoot.put(rootElement, node);
        return nodeWithRoot;
    }

    private static ObjectNode linksToNode(List<Link> links) {
        ObjectNode node = MAPPER.createObjectNode();
        for(Link link : links) {
            addlinkToNode(node, link);
        }
        return node;
    }

    private static ObjectNode addlinkToNode(ObjectNode node, Link link) {
        return node.put(link.getRel(), link.getPath());
    }

    public static class Summary { }

    public static class Tuple {
        private final Object object;
        private final Link link;
        public Tuple(Object object, Link link) {
            this.object = object;
            this.link = link;
        }

        public Object getObject() {
            return object;
        }

        public Link getLink() {
            return link;
        }
    }

    public static class InvalidJSONException extends Exception {

        public InvalidJSONException(String message) {
            super(message);
        }

        public InvalidJSONException() { }
    }
}
