package utilities;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

/**
 * Created by matthias on 6/03/2015.
 */
public class ControllerHelper {

    public static final ObjectNode EMPTY_RESULT;

    static {
        EMPTY_RESULT = Json.newObject();
        EMPTY_RESULT.put("status", "ok");
    }

    public static class Link {

        private String rel;
        private String path;

        public Link(String rel, String path) {
            this.rel = rel;
            this.path = path;
        }

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

}
