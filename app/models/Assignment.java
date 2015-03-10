package models;

import com.fasterxml.jackson.annotation.JsonRootName;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Benjamin on 5/03/2015.
 */
@Entity
@JsonRootName("assignment")
public class Assignment extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @ManyToMany(cascade = CascadeType.ALL)
    public List<Checkpoint> route;

    @Constraints.Required
    public int progress;

    @Constraints.Required
    public int priority;

    @OneToOne
    public User creator;

    @OneToOne
    public Drone assignedDrone;

    public Assignment(List<Checkpoint> route, User creator) {
        this();
        this.route = route;
        this.creator = creator;
    }

    public Assignment() {
        priority = 0;
        progress = 0;
    }

    public static Finder<Long,Assignment> find = new Finder<>(Long.class, Assignment.class);

}
