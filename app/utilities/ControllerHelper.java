package utilities;

/**
 * Created by matthias on 6/03/2015.
 */
public class ControllerHelper {

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

    public static class Summary { }

}
