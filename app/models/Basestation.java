package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import utilities.ControllerHelper;

import javax.persistence.*;

/**
 * Created by Eveline on 6/03/2015.
 */
@Entity
@JsonRootName("basestation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Basestation extends Model {

    @JsonView(ControllerHelper.Summary.class)
    @Id
    public Long id;

    @JsonView(ControllerHelper.Summary.class)
    @Constraints.Required
    @Column(length = 256, unique = true, nullable = false)
    public String name;

    @OneToOne(cascade = CascadeType.ALL)
    public Checkpoint checkpoint;

    public Basestation(String name, Checkpoint checkpoint){
        this.name = name;
        this.checkpoint = checkpoint;
    }

    public Basestation() { }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Basestation))
            return false;
        Basestation basestation = (Basestation) obj;
        return this.id.equals(basestation.id)
                && this.name.equals(basestation.name)
                && this.checkpoint.equals(basestation.checkpoint);
    }

    public static Finder<Long, Basestation> find = new Finder<>(Long.class, Basestation.class);
}
