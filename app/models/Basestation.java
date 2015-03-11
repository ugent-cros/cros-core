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

    public final static Finder<Long, Basestation> FIND = new Finder<>(Long.class, Basestation.class);

    @JsonView(ControllerHelper.Summary.class)
    @Id
    private Long id;

    @JsonView(ControllerHelper.Summary.class)
    @Constraints.Required
    @Column(length = 256, unique = true, nullable = false)
    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    private Checkpoint checkpoint;

    public Basestation(String name, Checkpoint checkpoint){
        this.name = name;
        this.checkpoint = checkpoint;
    }

    public Basestation() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }

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

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (checkpoint != null ? checkpoint.hashCode() : 0);
        return result;
    }
}
