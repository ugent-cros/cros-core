package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import play.db.ebean.Model;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthias on 3/03/2015.
 */

@MappedSuperclass
public abstract class Resource extends Model {

    private List<Link> links;

    @JsonProperty("links")
    public List<Link> getLinks() {
        if (links == null)
            links = getLinksList();
        return links;
    }

    @JsonIgnore
    public abstract List<Link> getLinksList();

    public class Link {

        public String rel;
        public String path;

        public Link(String rel, String path) {
            this.rel = rel;
            this.path = path;
        }
    }

}
